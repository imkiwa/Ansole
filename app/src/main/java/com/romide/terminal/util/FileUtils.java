package com.romide.terminal.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import android.content.Context;

public class FileUtils {

	public static void copyAssetsFile(Context context, String assetsFileName,
			String ouputFilePath) {
		try {
			if (!new File(ouputFilePath).exists()) {
				new File(ouputFilePath).createNewFile();
			}
			InputStream fromFileIs = context.getResources().getAssets()
					.open(assetsFileName);
			int length = 1024;
			byte[] buffer = new byte[length];
			FileOutputStream fileOutputStream = new FileOutputStream(
					ouputFilePath);
			BufferedInputStream bufferedInputStream = new BufferedInputStream(
					fromFileIs);
			BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(
					fileOutputStream);
			int len = bufferedInputStream.read(buffer);
			while (len != -1) {
				bufferedOutputStream.write(buffer, 0, len);
				len = bufferedInputStream.read(buffer);
			}
			bufferedInputStream.close();
			bufferedOutputStream.close();
			fromFileIs.close();
			fileOutputStream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	
	public static String getTextFromAssets(Context con,String fileName)
	{
		String result = ""; 
		try
		{
			InputStream in = con.getResources().getAssets().open(fileName); 
			int lenght = in.available();
			byte[] buffer = new byte[lenght];
			in.read(buffer); 
			result = new String(buffer, "utf-8");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return result;
	}
}
