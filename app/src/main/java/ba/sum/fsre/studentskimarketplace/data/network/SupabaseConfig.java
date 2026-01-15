package ba.sum.fsre.studentskimarketplace.data.network;

import ba.sum.fsre.studentskimarketplace.BuildConfig;

public final class SupabaseConfig {

    public static final String SUPABASE_URL = BuildConfig.SUPABASE_URL;
    public static final String SUPABASE_ANON_KEY = BuildConfig.SUPABASE_ANON_KEY;
    public static final String REST_BASE = SUPABASE_URL + "/rest/v1";

    private SupabaseConfig() {}
}
