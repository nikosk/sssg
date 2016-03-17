package sssg.domain;

import org.apache.commons.io.FilenameUtils;
import org.pegdown.PegDownProcessor;

import java.util.Map;

import static java.lang.String.format;

/**
 * sssg.domain
 * User: nk
 * Date: 2016-03-17 10:39
 */
public class Content {

	public String originalPath;

	public String markdown;

	public Map<String, String> meta;

	public String path;

	public Content(String originalPath, String markdown, Map<String, String> meta) {
		this.originalPath = originalPath;
		this.markdown = markdown;
		this.meta = meta;
		this.path = meta.getOrDefault("save_as", format("%s.html", FilenameUtils.getBaseName(originalPath)));
	}

	public String getOriginalPath() {
		return originalPath;
	}

	public void setOriginalPath(String originalPath) {
		this.originalPath = originalPath;
	}

	public String getMarkdown() {
		return markdown;
	}

	public void setMarkdown(String markdown) {
		this.markdown = markdown;
	}

	public Map<String, String> getMeta() {
		return meta;
	}

	public void setMeta(Map<String, String> meta) {
		this.meta = meta;
	}

	public String html() {
		return new PegDownProcessor().markdownToHtml(markdown);
	}

}
