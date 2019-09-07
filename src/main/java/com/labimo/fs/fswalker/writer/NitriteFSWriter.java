package com.labimo.fs.fswalker.writer;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;

import org.dizitart.no2.Document;
import org.dizitart.no2.Nitrite;
import org.dizitart.no2.NitriteCollection;
import org.dizitart.no2.objects.ObjectRepository;

import com.labimo.fs.fswalker.FSDirEntry;
import com.labimo.fs.fswalker.FSEntry;
import com.labimo.fs.fswalker.FSWriter;
import com.labimo.fs.fswalker.WriterInfo;

public class NitriteFSWriter implements FSWriter {

	private WriterInfo info;

	private Nitrite db = null;

	private EnumSet<OPTION> options;

	private NitriteCollection fsDocs;
	
	public final String FILEEXT="fsdata";
	
	public final static String FIELD_HASH="hash";
	public final static String FIELD_PATH="path";

	private static final String FIELD_CREATEDATE = "createtime";

	private static final String FIELD_SIZE = "size";

	private static final String FIELD_TYPE = "type";

	private static final String FIELD_ERROR = "error";

	private static final String FIELD_ACCESSDATE = "accesstime";

	private static final String FIELD_MODIFIEDDATE = "changetime";

	public NitriteFSWriter() {
		initInfo(null);		
		initDB(this.info);
	}

	public NitriteFSWriter(Nitrite db) {
		this.db=db;
		initInfo(null);
		initDB(this.info);
	}

	public NitriteFSWriter(WriterInfo info) {
		this.info=info;
		initInfo(info);
		initDB(this.info);
	}
	
	private void initInfo(WriterInfo info) {
		if (info==null) {
			info=new WriterInfo();
			Date now = new Date();
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-ddHHmmss");
			String shortName = format.format(new Date());
			info.setShortName(shortName);
			info.setCreateTime(now);
			info.setUpdateTime(now);
		}
		
		this.info=info;
	}

	private void initDB(WriterInfo info) {
		if (db==null) {
			db = Nitrite.builder().filePath(info.getShortName()+"."+FILEEXT).compressed().openOrCreate();
			ObjectRepository<WriterInfo> infoDocs = db.getRepository(WriterInfo.class);
			infoDocs.insert(info);
			infoDocs.close();	
		}
		
		fsDocs = db.getCollection(FSEntry.class.getSimpleName());

	}

	@Override
	public void write(FSEntry entry) throws UnsupportedEncodingException, IOException {
		
		if (entry==null) return;
		Document doc = Document.createDocument(FIELD_PATH,entry.getPath().toString());
		doc.put(FIELD_HASH,entry.getHash());
		doc.put(FIELD_CREATEDATE,entry.getDate().toMillis());
		doc.put(FIELD_SIZE,entry.getSize());
		doc.put(FIELD_TYPE,entry.getType());
		if (entry.getError()!=null) {
		doc.put(FIELD_ERROR,entry.getError().getMessage());
		}
		doc.put(FIELD_ACCESSDATE,entry.getAccesTime().toMillis());
		doc.put(FIELD_MODIFIEDDATE,entry.getModifiedTime().toMillis());
		fsDocs.insert(doc);
	}

	@Override
	public void close() {
		if (!fsDocs.isClosed()) fsDocs.close();
		if (!db.isClosed())
			db.close();
	}

	@Override
	public void setOptions(EnumSet<OPTION> options) {
		this.options=options;
	}

	@Override
	public EnumSet<OPTION> getOptions() {
		return this.options;
	}

	@Override
	public WriterInfo getInfo() {
		return info;
	}

}
