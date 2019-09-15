package com.example.android.bluetoothchat;

/*
 * Copyright 2016 Socket Mobile, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

/**
 * A utility class for getting the Bluetooth address
 * <p>
 * BluetoothUtils acts as a caching proxy for {@link BluetoothAdapter#getAddress()} so that your
 * application can continue to programmatically retrieve a device's Bluetooth address even after
 * the
 * device is upgraded to Android 6.0 (Marshmallow).
 */
public class BluetoothUtils {

    private static final String PREFS_FILE_KEY = BluetoothUtils.class.getName();

    private static final String PREF_BDADDRESS = "BdAddress";

    private static final String DUMMY_ADDRESS = "02:00:00:00:00:00";

    /**
     * Returns the Bluetooth address.
     * <p>
     * Tries to retrieve the Bluetooth address from {@SharedPreferences} before querying the
     * {@link BluetoothAdapter} and if the adapter returns a valid address, the address is saved in
     * {@link SharedPreferences} before it is returned.
     * <p>
     * This method creates a private instance of {@link SharedPreferences} for storing and
     * retrieving the Bluetooth address. To provide your own {@link SharedPreferences} instance,
     * use {@link #getAddress(Context, SharedPreferences, String)} instead.
     *
     * @param context an instance of {@link Context}
     * @return the Bluetooth address or null if Bluetooth is not supported
     */
    public static String getAddress(Context context) {
        return getAddress(context, getSharedPrefs(context), PREF_BDADDRESS);
    }

    /**
     * Returns the Bluetooth address.
     * <p>
     * Tries to retrieve the Bluetooth address from the instance of {@SharedPreferences} provided
     * before querying the {@link BluetoothAdapter} and if the adapter returns a valid address, the
     * address is saved in {@link SharedPreferences} before it is returned.
     *
     * @param context an instance of {@link Context}
     * @param prefs an instance of {@link SharedPreferences}
     * @param key the key for storing and retrieving the Bluetooth address in {@code prefs}
     */
    public static String getAddress(Context context, SharedPreferences prefs, String key) {
        if (prefs == null) {
            throw new IllegalArgumentException("SharedPreferences cannot be null");
        }

        String address = prefs.getString(key, null);

        if (address == null) {

            address = getAddressFromAdapter(context);

            if (!DUMMY_ADDRESS.equals(address)) {
                putAddressIntoSharedPrefs(prefs, address);
            }
        }

        return address;
    }

    private static BluetoothAdapter getAdapter(Context context) {

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return BluetoothAdapter.getDefaultAdapter();
        } else {
            BluetoothManager bm = (BluetoothManager) context
                    .getSystemService(Context.BLUETOOTH_SERVICE);
            if (bm != null) {
                return bm.getAdapter();
            }
        }

        return null;
    }

    private static String getAddressFromAdapter(Context context) {

        BluetoothAdapter adapter = getAdapter(context);

        if (adapter != null) {
            return adapter.getAddress();
        }

        return null;
    }

    private static SharedPreferences getSharedPrefs(Context context) {
        return context.getSharedPreferences(PREFS_FILE_KEY, Context.MODE_PRIVATE);
    }

    private static void putAddressIntoSharedPrefs(SharedPreferences prefs, String address) {

        SharedPreferences.Editor edit = prefs.edit();
        edit.putString(PREF_BDADDRESS, address);

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD) {
            edit.commit();
        } else {
            edit.apply();
        }
    }
}
