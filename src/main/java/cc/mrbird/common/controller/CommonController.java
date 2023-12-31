package cc.mrbird.common.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class CommonController {

	@RequestMapping("common/download")
	public void fileDownload(String fileName, Boolean delete, HttpServletResponse response,
							 HttpServletRequest request) {
		String realFileName = System.currentTimeMillis() + fileName.substring(fileName.indexOf("_") + 1);
		try {
			response.setCharacterEncoding("utf-8");
			response.setContentType("multipart/form-data");
			response.setHeader("Content-Disposition",
					"attachment;fileName=" + URLEncoder.encode(realFileName, StandardCharsets.UTF_8.toString()));
			String filePath = ResourceUtils.getURL("classpath:").getPath() + "static" + File.separator + "file" + File.separator + fileName;
			File file = new File(filePath);

			try (InputStream inputStream = new FileInputStream(file);
				 OutputStream os = response.getOutputStream()) {

				byte[] b = new byte[2048];
				int length;
				while ((length = inputStream.read(b)) > 0) {
					os.write(b, 0, length);
				}
			}

			if (delete && file.exists()) {
				file.delete();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
