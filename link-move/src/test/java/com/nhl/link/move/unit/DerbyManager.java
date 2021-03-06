package com.nhl.link.move.unit;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.apache.commons.io.FileUtils;

public class DerbyManager {

	public static final OutputStream DEV_NULL = new OutputStream() {

		@Override
		public void write(int b) {
		}
	};

	public DerbyManager(String location) {

		System.setProperty("derby.stream.error.field", DerbyManager.class.getName() + ".DEV_NULL");

		File derbyDir = new File(location);
		if (derbyDir.isDirectory()) {
			try {
				FileUtils.deleteDirectory(derbyDir);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public void shutdown() {
		try {
			DriverManager.getConnection("jdbc:derby:;shutdown=true");
		} catch (SQLException e) {
			// the exception is actually expected on shutdown... go figure...
		}
	}
}
