package fun.sast.mc.login.utils;

public interface PlayerAuth {
    boolean sastLogin$isAuthenticated();
    void sastLogin$setAuthenticated(boolean authenticated);
    void sastLogin$sendAuthMessage();
    void sastLogin$sendAuthOKMessage();
}
