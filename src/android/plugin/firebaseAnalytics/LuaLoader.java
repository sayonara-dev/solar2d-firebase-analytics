// Firebase Analytics for Solar2D (Android) with Consent Mode v2.
// Altered from com.solar2d-plugin.googleAnalytics.v2 (zlib license, see LICENSE.md):
// new Firebase BoM, setConsent, setCollectionEnabled, numbers keep their type.
package plugin.firebaseAnalytics;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.ansca.corona.CoronaEnvironment;
import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.naef.jnlua.JavaFunction;
import com.naef.jnlua.LuaState;
import com.naef.jnlua.LuaType;
import com.naef.jnlua.NamedJavaFunction;

import java.util.EnumMap;
import java.util.Map;

/**
 * Lua API (local fa = require "plugin.firebaseAnalytics"):
 *   fa.init()                        -> true / false
 *   fa.setConsent(granted)           -> AD_STORAGE, AD_USER_DATA, AD_PERSONALIZATION, ANALYTICS_STORAGE
 *   fa.setCollectionEnabled(enabled)
 *   fa.logEvent(name [, params])     -> params: string / number / boolean values
 *
 * Every call is wrapped: a Firebase failure is logged and never raises a Lua error.
 */
public class LuaLoader implements JavaFunction {
	private static final String TAG = "plugin.firebaseAnalytics";

	// Parameters that Google Analytics reads as money: always sent as double.
	private static final String[] DOUBLE_KEYS = { "value", "price", "tax", "shipping" };

	private FirebaseAnalytics analytics;

	@Override
	public int invoke(LuaState L) {
		NamedJavaFunction[] functions = new NamedJavaFunction[] {
			new InitFn(), new SetConsentFn(), new SetCollectionEnabledFn(), new LogEventFn(),
		};
		L.register(L.toString(1), functions);
		return 1;
	}

	private FirebaseAnalytics instance() {
		if (analytics == null) {
			Context context = CoronaEnvironment.getApplicationContext();
			if (context == null) {
				return null;
			}
			FirebaseApp.initializeApp(context);
			analytics = FirebaseAnalytics.getInstance(context);
		}
		return analytics;
	}

	private class InitFn implements NamedJavaFunction {
		@Override
		public String getName() { return "init"; }

		@Override
		public int invoke(LuaState L) {
			boolean ok = false;
			try {
				ok = instance() != null;
			} catch (Throwable e) {
				Log.e(TAG, "init failed", e);
			}
			L.pushBoolean(ok);
			return 1;
		}
	}

	private class SetConsentFn implements NamedJavaFunction {
		@Override
		public String getName() { return "setConsent"; }

		@Override
		public int invoke(LuaState L) {
			boolean granted = L.toBoolean(1);
			try {
				FirebaseAnalytics fa = instance();
				if (fa != null) {
					FirebaseAnalytics.ConsentStatus status = granted
						? FirebaseAnalytics.ConsentStatus.GRANTED
						: FirebaseAnalytics.ConsentStatus.DENIED;
					Map<FirebaseAnalytics.ConsentType, FirebaseAnalytics.ConsentStatus> consent =
						new EnumMap<FirebaseAnalytics.ConsentType, FirebaseAnalytics.ConsentStatus>(FirebaseAnalytics.ConsentType.class);
					consent.put(FirebaseAnalytics.ConsentType.AD_STORAGE, status);
					consent.put(FirebaseAnalytics.ConsentType.AD_USER_DATA, status);
					consent.put(FirebaseAnalytics.ConsentType.AD_PERSONALIZATION, status);
					consent.put(FirebaseAnalytics.ConsentType.ANALYTICS_STORAGE, status);
					fa.setConsent(consent);
				}
			} catch (Throwable e) {
				Log.e(TAG, "setConsent failed", e);
			}
			return 0;
		}
	}

	private class SetCollectionEnabledFn implements NamedJavaFunction {
		@Override
		public String getName() { return "setCollectionEnabled"; }

		@Override
		public int invoke(LuaState L) {
			boolean enabled = L.toBoolean(1);
			try {
				FirebaseAnalytics fa = instance();
				if (fa != null) {
					fa.setAnalyticsCollectionEnabled(enabled);
				}
			} catch (Throwable e) {
				Log.e(TAG, "setCollectionEnabled failed", e);
			}
			return 0;
		}
	}

	private class LogEventFn implements NamedJavaFunction {
		@Override
		public String getName() { return "logEvent"; }

		@Override
		public int invoke(LuaState L) {
			if (!L.isString(1)) {
				Log.w(TAG, "logEvent: event name must be a string");
				return 0;
			}
			String name = L.toString(1);
			Bundle params = new Bundle();
			if (L.type(2) == LuaType.TABLE) {
				L.pushNil();
				while (L.next(2)) {
					// key at -2, value at -1; isString() is true for numbers too, so check the type
					if (L.type(-2) == LuaType.STRING) {
						putParam(params, L.toString(-2), L);
					}
					L.pop(1);
				}
			}
			try {
				FirebaseAnalytics fa = instance();
				if (fa != null) {
					fa.logEvent(name, params);
				}
			} catch (Throwable e) {
				Log.e(TAG, "logEvent failed", e);
			}
			return 0;
		}
	}

	private static void putParam(Bundle params, String key, LuaState L) {
		LuaType type = L.type(-1);
		if (type == LuaType.BOOLEAN) {
			// GA has no boolean parameters; 1 / 0 like the official SDKs do
			params.putLong(key, L.toBoolean(-1) ? 1L : 0L);
		} else if (type == LuaType.NUMBER) {
			double number = L.toNumber(-1);
			if (isDoubleKey(key) || number != Math.rint(number) || Double.isInfinite(number)) {
				params.putDouble(key, number);
			} else {
				params.putLong(key, (long) number);
			}
		} else if (type == LuaType.STRING) {
			params.putString(key, L.toString(-1));
		}
	}

	private static boolean isDoubleKey(String key) {
		for (String k : DOUBLE_KEYS) {
			if (k.equals(key)) {
				return true;
			}
		}
		return false;
	}
}
