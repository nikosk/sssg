package sssg.domain;

import java.util.List;

/**
 * sssg.domain
 * User: nk
 * Date: 2016-03-17 10:32
 */
public class Category {

	public String name;
	public String path;
	public List<Content> articles;

	public Category(String name, String path, List<Content> articles) {
		this.name = name;
		this.path = path;
		this.articles = articles;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public Iterable<Content> getArticles() {
		return articles;
	}

	public void setArticles(List<Content> articles) {
		this.articles = articles;
	}
}
