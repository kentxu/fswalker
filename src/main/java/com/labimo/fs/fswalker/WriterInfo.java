package com.labimo.fs.fswalker;

import java.util.Date;

/**
 * Writer Metadata
 *
 */
public class WriterInfo {

	private String name;
	private Date createTime;
	private Date updateTime;
	/**
	 * a simple identification. Only alphanumeric, '-' or '_' are allowed
	 */
	private String shortName;
	private String description;
	private String[] paths;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Date getCreateTime() {
		return createTime;
	}
	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}
	public Date getUpdateTime() {
		return updateTime;
	}
	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}
	public String getShortName() {
		return shortName;
	}
	public void setShortName(String shortName) {
		shortName=shortName.replaceAll("[^a-zA-Z0-9\\-\\_]", "");
		this.shortName = shortName;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String[] getPaths() {
		return paths;
	}
	public void setPaths(String[] paths) {
		this.paths = paths;
	}
	
}
