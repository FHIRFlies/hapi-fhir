package org.hl7.fhir.convertors;

import org.bson.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class BsonHelper {

	public List<String> Read(String filename) throws FileNotFoundException {
		File file = new File(filename);
		InputStream inputStream = new BufferedInputStream(new FileInputStream(file));

		List<String> jsonList = new ArrayList<String>();

		BSONDecoder decoder = new BasicBSONDecoder();
		int count = 0;
		try {
			while (inputStream.available() > 0) {

				BSONObject obj = decoder.readObject(inputStream);
				if(obj == null){
					break;
				}
				jsonList.add(obj.toString());
				count++;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				inputStream.close();
			} catch (IOException e) {
			}
		}
		System.err.println(String.format("%s objects read", count));

		return jsonList;
	}

	public void Write(String filename,List<BSONObject> content) throws Exception {
		File file = new File(filename);
		OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file,true),4096);


		BSONEncoder encoder = new BasicBSONEncoder();

		try {
			for (BSONObject obj: content
				  ) {
				outputStream.write(encoder.encode(obj));
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				outputStream.close();
			} catch (IOException e) {
			}
		}
		System.out.println("Result written to Bson File");
	}
}
