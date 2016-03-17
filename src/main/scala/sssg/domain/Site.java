package sssg.domain;

import java.util.Map;

/**
 * sssg.domain
 * User: nk
 * Date: 2016-03-17 02:40
 */
public class Site {

	public Map<String, Object> meta;
	public Iterable<Content> pages;
	public Iterable<Category> categories;

	public Site(Map<String, Object> meta, Iterable<Content> pages, Iterable<Category> categories) {
		this.meta = meta;
		this.pages = pages;
		this.categories = categories;
	}

	public Map<String, Object> getMeta() {
		return meta;
	}

	public void setMeta(Map<String, Object> meta) {
		this.meta = meta;
	}

	public Iterable<Content> getPages() {
		return pages;
	}

	public void setPages(Iterable<Content> pages) {
		this.pages = pages;
	}

	public Iterable<Category> getCategories() {
		return categories;
	}

	public void setCategories(Iterable<Category> categories) {
		this.categories = categories;
	}
}
