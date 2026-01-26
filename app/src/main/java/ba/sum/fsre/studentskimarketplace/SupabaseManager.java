package ba.sum.fsre.studentskimarketplace;

import io.github.jan.supabase.SupabaseClient;
import io.github.jan.supabase.SupabaseClientBuilder;
import io.github.jan.supabase.gotrue.Auth;
import io.github.jan.supabase.gotrue.AuthKt;
import io.github.jan.supabase.postgrest.Postgrest;
import io.github.jan.supabase.postgrest.PostgrestKt;
import io.github.jan.supabase.realtime.Realtime;
import io.github.jan.supabase.realtime.RealtimeKt;

public class SupabaseManager {
    private static SupabaseClient client;

    public static SupabaseClient getClient() {
        if (client == null) {
            SupabaseClientBuilder builder = new SupabaseClientBuilder(
                    BuildConfig.SUPABASE_URL,
                    BuildConfig.SUPABASE_ANON_KEY
            );

            builder.install(Auth.Companion, config -> null);
            builder.install(Postgrest.Companion, config -> null);
            builder.install(Realtime.Companion, config -> null);

            client = builder.build();
        }
        return client;
    }

    public static Auth getAuth() {
        return AuthKt.getAuth(getClient());
    }

    public static Postgrest getPostgrest() {
        return PostgrestKt.getPostgrest(getClient());
    }
}