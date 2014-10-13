package jp.daneko.android.inappv3.exception;

/**
 * 事実上Runtimeなのでは感
 */
public class IabException extends Exception {
    public IabException(String detailMessage) {
        super(detailMessage);
    }
}
