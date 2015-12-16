package enlarger;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
import org.imgscalr.Scalr;

public class FixIconsProcessor
{
	private static final Logger logger = Logger.getGlobal();
	
	private float resizeFactor;
	private boolean saveGifInPngFormat;

	public FixIconsProcessor(float resizeFactor, boolean saveGifInPngFormat) {
		this.resizeFactor = resizeFactor;
		this.saveGifInPngFormat = saveGifInPngFormat;
	}
	
	public void process(File directory, File outputDirectory, int parallelThreads)
			throws Exception {

		ExecutorService threadPool = (parallelThreads > 1)
				? Executors.newFixedThreadPool(parallelThreads)
				: null;

		processDirectory(threadPool, directory, outputDirectory);

		if (threadPool != null) {
			threadPool.shutdown();
			threadPool.awaitTermination(1, TimeUnit.DAYS);
		}
	}
	
	private void processDirectory(ExecutorService threadPool, File directory, File outputDirectory)
			throws Exception {
		logger.info("Processing directory [" + directory.getAbsolutePath()
				+ "]");

		for (File file : directory.listFiles()) {
			if (file.isDirectory()) {
				File targetDir = new File(outputDirectory.getAbsolutePath()
						+ File.separator + file.getName());
				logger.info("Creating directory: "
						+ targetDir.getAbsolutePath());
				targetDir.mkdir();
				processDirectory(threadPool, file, targetDir);
			} else {
				File targetFile = new File(outputDirectory.getAbsolutePath()
						+ File.separator + file.getName());

				if (file.getName().toLowerCase().endsWith(".zip")
						|| file.getName().toLowerCase().endsWith(".jar")) {

					Runnable runable = () -> {
						logger.info("Processing archive file: "
								+ file.getAbsolutePath());

						try {
							processArchive(file, targetFile);
						} catch (Exception e) {
							logger.log(Level.SEVERE, "Unexpected error in processing archive " + file.getAbsolutePath() + ": " + e.getMessage(), e);
						}
					};

					if (threadPool != null)
						threadPool.execute(runable);
					else
						runable.run();
				} else if (ImageType.findType(file.getName()) != null) {
					logger.info("Processing image: " + file.getAbsolutePath());

					FileInputStream inStream = null;
					FileOutputStream outStream = null;

					try {
						inStream = new FileInputStream(file);
						outStream = new FileOutputStream(targetFile);
						processImage(file.getName(), inStream, outStream);
					} finally {
						IOUtils.closeQuietly(inStream);
						IOUtils.closeQuietly(outStream);
					}
				} else {
					logger.info("Processing : " + file.getAbsolutePath());

					FileInputStream inStream = null;
					FileOutputStream outStream = null;

					try {
						inStream = new FileInputStream(file);
						outStream = new FileOutputStream(targetFile);
						IOUtils.copy(inStream, outStream);
					} finally {
						IOUtils.closeQuietly(inStream);
						IOUtils.closeQuietly(outStream);
					}

				}

			}

		}

	}

	private void processArchive(File file, File targetFile)
			throws Exception {

		ZipFile zipSrc = null;
		ZipOutputStream outStream = null;
		try
		{
			zipSrc = new ZipFile(file);
			Enumeration<? extends ZipEntry> srcEntries = zipSrc.entries();

			outStream = new ZipOutputStream(
					new FileOutputStream(targetFile));

			while (srcEntries.hasMoreElements()) {
				ZipEntry entry = (ZipEntry) srcEntries.nextElement();
				logger.info("Processing zip entry [" + entry.getName()
						+ "]");

				ZipEntry newEntry = new ZipEntry(entry.getName());
				try {
					outStream.putNextEntry(newEntry);
				} catch (Exception e) {
					if (!e.getMessage().startsWith("duplicate entry: ")) {
						logger.log(Level.SEVERE, "Error: ", e);
					} else {
						logger.log(Level.SEVERE, e.getMessage(), e);
					}
					outStream.closeEntry();
					continue;
				}

				BufferedInputStream bis = new BufferedInputStream(
						zipSrc.getInputStream(entry));

				if (ImageType.findType(entry.getName()) != null) {
					processImage(zipSrc.getName() + "!/" + entry.getName(), bis, outStream);
				} else {
					IOUtils.copy(bis, outStream);
				}

				outStream.closeEntry();
				bis.close();
			}

		} catch (Exception e)
		{
			logger.log(Level.SEVERE, "Can't process file: "+file.getAbsolutePath(), e);
		}
		finally
		{
			try
			{
				if(zipSrc!=null) zipSrc.close();
				if(outStream!=null) outStream.close();
			} catch (IOException e)
			{
				logger.log(Level.SEVERE, "Can't close zip streams for file: "+file.getAbsolutePath(), e);
			}
		}
	}

	public void processImage(String fileName, InputStream input,
			OutputStream output) throws IOException {

		logger.info("Scaling image: " + fileName);

		boolean imageWriteStarted = false;
		try {
			BufferedImage out = ImageIO.read(input);

			int outWidth = (int) (out.getWidth() * resizeFactor);
			int outHeight = (int) (out.getHeight() * resizeFactor);

			BufferedImage rescaledOut = createResizedCopy(out, outWidth, outHeight);

			String imageFormatName = ImageType.findType(fileName).name();
			if (saveGifInPngFormat && "GIF".equals(imageFormatName))
				imageFormatName = "PNG";
			ImageIO.write(rescaledOut, imageFormatName, output);

		} catch (Exception e) {
			if (imageWriteStarted) {
				throw new RuntimeException("Failed to scale image [" + fileName
						+ "]: " + e.getMessage(), e);
			} else {
				logger.log(Level.SEVERE,
						"Unable to scale [" + fileName + "]: " + e.getMessage(),
						e);
				IOUtils.copy(input, output);
			}
		}
	}

	private BufferedImage createResizedCopy(BufferedImage originalImage, int scaledWidth, int scaledHeight) {
		
		BufferedImage scaledBI = Scalr.resize(originalImage, Scalr.Method.QUALITY, scaledWidth, scaledHeight);
		return scaledBI;
	}
}
