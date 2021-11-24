package com.arafat.filemanager;

//this is for file attributes
public class SharedFile{

        String fileName;
        int fileID;
        int fileSize;
        String fileType;
        String visibility;
        String filePath;
        int ownerId;

        public String getFileName() {
                return fileName;
        }

        public SharedFile setFileName(String fileName) {
                this.fileName = fileName;
                return this;
        }

        public String getFilePath() {
                return filePath;
        }

        public SharedFile setFilePath(String filePath) {
                this.filePath = filePath;
                return this;
        }

        public int getFileID() {
                return fileID;
        }

        public SharedFile setFileID(int fileID) {
                this.fileID = fileID;
                return this;
        }

        public int getFileSize() {
                return fileSize;
        }

        public SharedFile setFileSize(int fileSize) {
                this.fileSize = fileSize;
                return this;
        }

        public String getFileType() {
                return fileType;
        }

        public SharedFile setFileType(String fileType) {
                this.fileType = fileType;
                return this;
        }

        public String getVisibility() {
                return visibility;
        }

        public SharedFile setVisibility(String visibility) {
                this.visibility = visibility;
                return this;
        }

        public int getOwnerId() {
                return ownerId;
        }

        public SharedFile setOwnerID(int ownerId) {
                this.ownerId = ownerId;
                return this;
        }
}
