/**
 * 
 */
package tkv;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.hadoop.fs.FileSystem;

import tkv.hdfs.HdfsDataStore;
import tkv.hdfs.HdfsIndexStore;

/**
 * @author sean.wang
 * @since Mar 7, 2012
 */
public class HdfsImpl implements Tkv {
	private HdfsIndexStore indexStore;

	private HdfsDataStore dataStore;

	private Lock writeLock = new ReentrantLock();

	public HdfsImpl(FileSystem fs, File localDir, String indexFilename, String dataFilename, int keyLength, int tagLength) throws IOException {
		File localIndexFile = new File(localDir, indexFilename);
		if (!localDir.exists()) {
			boolean rs = localDir.mkdirs();
			if (!rs) {
				throw new IOException("can't create local dir!");
			}
		}
		if (!localIndexFile.exists()) {
			boolean rs = localIndexFile.createNewFile();
			if (!rs) {
				throw new IOException("can't create local index file!");
			}
		}
		this.setIndexStore(new HdfsIndexStore(fs, indexFilename, localIndexFile, keyLength, tagLength));
		this.setDataStore(new HdfsDataStore(fs, dataFilename));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.dianping.tkv.hdfs.Tkv#close()
	 */
	@Override
	public void close() throws IOException {
		try {
			writeLock.lock();
			this.indexStore.close();
			this.dataStore.close();
		} finally {
			writeLock.unlock();
		}
	}

	/*
	 * 
	 * 
	 * @see com.dianping.tkv.hdfs.Tkv#get(int)
	 */
	@Override
	public byte[] get(int indexPos) throws IOException {
		Meta meta = this.indexStore.getIndex(indexPos);
		if (meta == null) {
			return null;
		}
		return getValue(meta);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.dianping.tkv.hdfs.Tkv#get(java.lang.String)
	 */
	@Override
	public byte[] get(String key) throws IOException {
		Meta meta = getIndex(key);
		if (meta == null) {
			return null;
		}
		return getValue(meta);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.dianping.tkv.hdfs.Tkv#get(java.lang.String, java.lang.String)
	 */
	@Override
	public byte[] get(String key, String tag) throws IOException {
		Meta meta = getIndex(key, tag);
		if (meta == null) {
			return null;
		}
		return getValue(meta);
	}

	public DataStore getDataStore() {
		return dataStore;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.dianping.tkv.hdfs.Tkv#getIndex(int)
	 */
	@Override
	public Meta getIndex(int indexPos) throws IOException {
		return this.indexStore.getIndex(indexPos);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.dianping.tkv.hdfs.Tkv#getIndex(java.lang.String)
	 */
	@Override
	public Meta getIndex(String key) throws IOException {
		return this.indexStore.getIndex(key);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.dianping.tkv.hdfs.Tkv#getIndex(java.lang.String, java.lang.String)
	 */
	@Override
	public Meta getIndex(String key, String tag) throws IOException {
		return this.indexStore.getIndex(key, tag);
	}

	public IndexStore getIndexStore() {
		return indexStore;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.dianping.tkv.hdfs.Tkv#getRecord(java.lang.String, java.lang.String)
	 */
	@Override
	public Record getRecord(String key, String tag) throws IOException {
		throw new UnsupportedOperationException();
	}

	/**
	 * @param index
	 * @return
	 * @throws IOException
	 */
	private byte[] getValue(Meta meta) throws IOException {
		return this.dataStore.get(meta.getOffset(), meta.getLength());
	}

	private List<Meta> metas = new ArrayList<Meta>();

	public void buildIndex() throws IOException {
		try {
			writeLock.lock();
			if (metas.size() == 0) {
				return;
			}
			Collections.sort(metas, new Comparator<Meta>() {

				@Override
				public int compare(Meta o1, Meta o2) {
					return o1.getKey().compareTo(o2.getKey());
				}
			});
			for (int i = 0; i < metas.size(); i++) {
				Meta meta = metas.get(i);
				Map<String, Tag> tags = meta.getTags();
				if (tags != null) {
					for (Tag t : tags.values()) {
						t.setPos(i);
						Tag holdTag = lastTagHolder.get(t.getName());
						if (holdTag != null) {
							t.setPrevious(holdTag.getPos());
							holdTag.setNext(i);
						}
						lastTagHolder.put(t.getName(), t);
					}
				}
			}
			for (Meta meta : metas) {
				this.indexStore.append(meta);
			}
			this.metas.clear();
		} finally {
			writeLock.unlock();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.dianping.tkv.hdfs.Tkv#put(java.lang.String, byte[])
	 */
	@Override
	public boolean put(String key, byte[] value) throws IOException {
		return this.put(key, value, (String[]) null);
	}

	Map<String, Tag> lastTagHolder = new HashMap<String, Tag>();

	@Override
	public boolean put(String key, byte[] value, String... tagNames) throws IOException {
		try {
			this.writeLock.lock();
			if (this.indexStore.getIndex(key) != null) {
				return false; // this key already exists
			}
			long offset = this.dataStore.length();
			this.dataStore.append(value);
			Meta meta = new Meta();
			meta.setKey(key);
			meta.setOffset(offset);
			meta.setLength(value.length);
			if (tagNames != null) {
				for (String tagName : tagNames) {
					meta.addTag(tagName);
				}
			}
			metas.add(meta);
			return true;
		} finally {
			this.writeLock.unlock();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.dianping.tkv.hdfs.Tkv#size()
	 */
	@Override
	public long size() throws IOException {
		return this.indexStore.size();
	}

	public void startWrite() throws IOException {
		this.dataStore.openOutput();
	}

	public void endWrite() throws IOException {
		this.indexStore.flush();
		this.dataStore.flushAndCloseOutput();
	}

	public void startRead() throws IOException {
		this.dataStore.openInput();
	}

	public void endRead() throws IOException {
		this.dataStore.closeInput();
	}

	public void setDataStore(HdfsDataStore dataStore) {
		this.dataStore = dataStore;
	}

	public void setIndexStore(HdfsIndexStore indexStore) {
		this.indexStore = indexStore;
	}

	@Override
	public boolean delete() throws IOException {
		boolean dataDeleted = this.dataStore.delete();
		boolean indexDeleted = this.indexStore.delete();
		return dataDeleted && indexDeleted;
	}

	public boolean deleteLocal() throws IOException {
		boolean dataDeleted = this.dataStore.deleteLocal();
		boolean indexDeleted = this.indexStore.deleteLocal();
		return dataDeleted && indexDeleted;
	}

	public boolean deleteRemote() throws IOException {
		boolean dataDeleted = this.dataStore.deleteRemote();
		boolean indexDeleted = this.indexStore.deleteRemote();
		return dataDeleted && indexDeleted;
	}

	@Override
	public byte[] getNext(String key, String tagName) throws IOException {
		Meta meta = this.getIndex(key, tagName);
		if (meta == null) {
			return null;
		}
		Map<String, Tag> tags = meta.getTags();
		if (tags == null) {
			return null;
		}
		Tag tag = tags.get(tagName);
		if (tag == null) {
			return null;
		}
		return this.get(tag.getNext());
	}

	@Override
	public byte[] getPrevious(String key, String tagName) throws IOException {
		Meta meta = this.getIndex(key, tagName);
		if (meta == null) {
			return null;
		}
		Map<String, Tag> tags = meta.getTags();
		if (tags == null) {
			return null;
		}
		Tag tag = tags.get(tagName);
		if (tag == null) {
			return null;
		}
		return this.get(tag.getPrevious());
	}

}
