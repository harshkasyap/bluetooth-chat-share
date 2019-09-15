package com.example.android.bluetoothchat;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class FileShareManagement {

    Context context;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothChatService mChatService = null;

    public FileShareManagement(BluetoothAdapter _mBluetoothAdapter, BluetoothChatService _mChatService, Context _context) throws Exception {
        context = _context;
        mBluetoothAdapter = _mBluetoothAdapter;
        mChatService = _mChatService;
    }

    public static int stringCompare(String str1, String str2)
    {
        int l1 = str1.length();
        int l2 = str2.length();
        int lmin = Math.min(l1, l2);

        for (int i = 0; i < lmin; i++) {
            int str1_ch = (int)str1.charAt(i);
            int str2_ch = (int)str2.charAt(i);

            if (str1_ch != str2_ch) {
                return str1_ch - str2_ch;
            }
        }

        if (l1 != l2) {
            return l1 - l2;
        }

        else {
            return 0;
        }
    }

    public String computeNearestOverlay(String fileId) {
        DataBase dataBase = new DataBase(context);
        dataBase.open();
        String nodes = dataBase.showAllNodes();
        dataBase.close();

        HashMap<String, Integer> devices = new HashMap<String, Integer>();

        String node [] = nodes.split("\n");
        for (String n: node) {
            String devId = n.split(";")[0];
            String devName = n.split(";")[1];
            String devAdd = n.split(";")[2];
            devices.put(devId+";"+devName+";"+devAdd, stringCompare(devId, fileId));
        }

        Map.Entry<String, Integer> min = null;
        for (Map.Entry<String, Integer> entry : devices.entrySet()) {
            if (min == null || min.getValue() > entry.getValue()) {
                min = entry;
            }
        }

        return min.getKey();
    }

    public void fileShare(String readMessage) {
        if (readMessage.toLowerCase().startsWith("sharefile")) {
            //Compute the Nearest Neighbour and send the overlayId to sender.
            String neareastOverlayId = computeNearestOverlay(readMessage.split(";")[2]);

            DataBase dataBase = new DataBase(context);
            dataBase.open();
            dataBase.addFileInfo(readMessage.split(";")[1], readMessage.split(";")[2], neareastOverlayId);
            dataBase.close();

            String msg = "Send File to : "+ neareastOverlayId;
            byte[] send = msg.getBytes();
            mChatService.write(send);
        }
        if (readMessage.toLowerCase().startsWith("getfile:")) {
            String neareastOverlayId = computeNearestOverlay(NodeManagement.hashSHA256(readMessage.split(":")[1]));

            String msg = "Get File From : "+ neareastOverlayId;
            byte[] send = msg.getBytes();
            mChatService.write(send);
            //
        }
    }

}
