package ba.sum.fsre.studentskimarketplace.data.session;

public final class AuthSession {
    private AuthSession() {}

    public static String accessToken = null;
    public static String userId = null;

    public static boolean isLoggedIn() {
        return accessToken != null && !accessToken.trim().isEmpty()
                && userId != null && !userId.trim().isEmpty();
    }
}
