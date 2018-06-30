package cn.lzq.zbarscanner.zbar;

/**
 * 扫码结果
 */
public class Result {
    private String mContents;
    public void setContents(String contents) {
        mContents = contents;
    }
    public String getContents() {
        return mContents;
    }
}