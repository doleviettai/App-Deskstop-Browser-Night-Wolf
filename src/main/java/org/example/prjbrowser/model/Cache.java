package org.example.prjbrowser.model;

import java.sql.Timestamp;
import java.util.Arrays;

public class Cache {
    private int id;
    private int urlId;
    private int userId;
    private String resourceUrl;
    private String etag;
    private Timestamp lastModified;
    private byte[] content;
    private String contentType;
    private Timestamp recvTime;
    private Timestamp expireTime;
    private int size;

    // ===== Constructors =====
    public Cache() {
    }

    public Cache(int id, int urlId, int userId, String resourceUrl, String etag,
                 Timestamp lastModified, byte[] content, String contentType,
                 Timestamp recvTime, Timestamp expireTime, int size) {
        this.id = id;
        this.urlId = urlId;
        this.userId = userId;
        this.resourceUrl = resourceUrl;
        this.etag = etag;
        this.lastModified = lastModified;
        this.content = content;
        this.contentType = contentType;
        this.recvTime = recvTime;
        this.expireTime = expireTime;
        this.size = size;
    }

    // ===== Getters & Setters =====
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUrlId() {
        return urlId;
    }

    public void setUrlId(int urlId) {
        this.urlId = urlId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getResourceUrl() {
        return resourceUrl;
    }

    public void setResourceUrl(String resourceUrl) {
        this.resourceUrl = resourceUrl;
    }

    public String getEtag() {
        return etag;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }

    public Timestamp getLastModified() {
        return lastModified;
    }

    public void setLastModified(Timestamp lastModified) {
        this.lastModified = lastModified;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Timestamp getRecvTime() {
        return recvTime;
    }

    public void setRecvTime(Timestamp recvTime) {
        this.recvTime = recvTime;
    }

    public Timestamp getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(Timestamp expireTime) {
        this.expireTime = expireTime;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    // ===== Debug / Logging =====
//    @Override
//    public String toString() {
//        return "Cache{id=\{id}, urlId=\{urlId}, userId=\{userId}, resourceUrl='\{resourceUrl != null ? resourceUrl.substring(0, Math.min(50, resourceUrl.length())) + "..." : null}', etag='\{etag}', lastModified=\{lastModified}, content=\{content != null ? content.length + " bytes" : "null"}, contentType='\{contentType}', recvTime=\{recvTime}, expireTime=\{expireTime}, size=\{size}}";
//    }
}
