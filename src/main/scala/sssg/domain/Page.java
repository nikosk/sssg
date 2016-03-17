package sssg.domain;

import java.util.List;

/**
 * sssg.domain
 * User: nk
 * Date: 2016-03-17 11:03
 */
public class Page {

	public Integer num;
	public List<Content> articles;
	public Boolean hasNext;
	public Boolean hasPrevious;

	public Page(Integer num, List<Content> articles, Boolean hasNext, Boolean hasPrevious) {
		this.num = num;
		this.articles = articles;
		this.hasNext = hasNext;
		this.hasPrevious = hasPrevious;
	}

	public Integer getNum() {
		return num;
	}

	public void setNum(Integer num) {
		this.num = num;
	}

	public List<Content> getArticles() {
		return articles;
	}

	public void setArticles(List<Content> articles) {
		this.articles = articles;
	}

	public Boolean getHasNext() {
		return hasNext;
	}

	public void setHasNext(Boolean hasNext) {
		this.hasNext = hasNext;
	}

	public Boolean getHasPrevious() {
		return hasPrevious;
	}

	public void setHasPrevious(Boolean hasPrevious) {
		this.hasPrevious = hasPrevious;
	}
}
