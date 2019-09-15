package com.example.android.bluetoothchat;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ContentQueryMap;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.widget.Toast;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.digests.SHA256Digest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.prefs.NodeChangeEvent;

import it.unisa.dia.gas.crypto.jpbc.fe.abe.gghsw13.params.GGHSW13MasterSecretKeyParameters;
import it.unisa.dia.gas.crypto.jpbc.signature.bls01.engines.BLS01Signer;
import it.unisa.dia.gas.crypto.jpbc.signature.bls01.generators.BLS01KeyPairGenerator;
import it.unisa.dia.gas.crypto.jpbc.signature.bls01.generators.BLS01ParametersGenerator;
import it.unisa.dia.gas.crypto.jpbc.signature.bls01.params.BLS01KeyGenerationParameters;
import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Field;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.plaf.jpbc.field.curve.CurveElement;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;
import com.google.common.io.BaseEncoding;

import static com.example.android.bluetoothchat.BluetoothChatService.STATE_CONNECTED;
import static com.example.android.bluetoothchat.BluetoothChatService.STATE_CONNECTING;

public class NodeManagement {
    Context context;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothChatService mChatService = null;
    FileShareManagement fileShareManagement = null;

    public NodeManagement(String add, BluetoothAdapter _mBluetoothAdapter,BluetoothChatService _mChatService, Context _context) throws Exception {
        this(add, _mBluetoothAdapter, _mChatService, _context, "mine");
    }

    public byte[] sign(String message, CipherParameters privateKey) {
        byte[] bytes = message.getBytes();
        BLS01Signer signer = new BLS01Signer(new SHA256Digest());
        signer.init(true, privateKey);
        signer.update(bytes, 0, bytes.length);
        byte[] signature = null;
        try {
            signature = signer.generateSignature();
        } catch (CryptoException e) {
            throw new RuntimeException(e);
        }
        return signature;
    }

    public boolean verify(byte[] signature, String message, CipherParameters publicKey) {
        byte[] bytes = message.getBytes();
        BLS01Signer signer = new BLS01Signer(new SHA256Digest());
        signer.init(false, publicKey);
        signer.update(bytes, 0, bytes.length);
        return signer.verifySignature(signature);
    }

    public static String objectToString(Object obj) {
        String str = "";
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(obj);
            oos.flush();
            byte [] data = bos.toByteArray();
            //str = Base64.getEncoder().encodeToString(data);
        } catch (Exception e) {
            str = e.toString();
        }
        return str;
    }

    public static Object stringToObject(String str) {
        try {
            //ByteArrayInputStream in = new ByteArrayInputStream(Base64.getDecoder().decode(str));
            //ObjectInputStream is = new ObjectInputStream(in);
            //return is.readObject();
        } catch (Exception e) {
            System.out.println("Exception : "+ e.toString());
        }
        return null;
    }

    public static float getEnergy(Context context) {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);

        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        float batteryPct = level / (float)scale;

        return  batteryPct;
    }

    public String generatePubPrivKey() {
        String key= "";
        try {
            Pairing pairing = PairingFactory.getPairing("assets/a.properties");
            PairingFactory.getInstance().setUsePBCWhenPossible(true);

            //*** For MS1 ****
            Element G = pairing.getG1().newRandomElement();
            // Generate the secret key
            Element sk = pairing.getZr().newRandomElement();

            G.setToRandom();
            sk.setToRandom();

            // Generate the corresponding public key
            Element pk = G.duplicate().mulZn(sk); // We need to duplicate g because it's a system parameter.

            // key = objectToString(sk) +  ";" + objectToString(pk);
            key = sk.toString() +  ";" + pk.toString();

//            Toast.makeText(context, key, Toast.LENGTH_LONG).show();

        } catch(Exception e) {
            Toast.makeText(context, e.toString(), Toast.LENGTH_LONG).show();
        }

        return key;
    }

    public NodeManagement(String add, BluetoothAdapter _mBluetoothAdapter,BluetoothChatService _mChatService, Context _context, String deviceDesc) throws Exception {
        context = _context;
        mBluetoothAdapter = _mBluetoothAdapter;
        mChatService = _mChatService;

        try {
            if (add.length() == 17) {
                DataBase dataBase = new DataBase(context);
                dataBase.open();

                String devHash = hashSHA256(add);

                String splitAddArr[] = dataBase.getHead("mine").split(";");
                String deviceAddress = "";
                if (splitAddArr.length > 2 && deviceDesc == "mine")  {
                    deviceAddress = splitAddArr[2].trim();
                    if (deviceAddress != add) {
                        dataBase.updateAddNOverlayId(add, devHash);
                    }
                } else {
                    String keys[] = generatePubPrivKey().split(";");

                    // In Start Self Id be overlay Id, After knowing Head will update this.
                    dataBase.addNode(devHash, mBluetoothAdapter.getName(), add, devHash,deviceDesc, keys[0].trim(), keys[1].trim());
                }
                dataBase.close();
            } else {
                // Shouldn't come here.
            }
        } catch (Exception e) {
            Toast.makeText(context, e.toString(), Toast.LENGTH_LONG).show();
        }

    }

    public static String hashSHA256(String stringToHash) {
        String hashedString = "";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] messageDigest = md.digest(stringToHash.getBytes());
            BigInteger no = new BigInteger(1, messageDigest);
            hashedString = no.toString(16);
        } catch (Exception e){}

        return hashedString;
    }

    public void setUpDevice(BluetoothDevice device) {
        mChatService.connect(device, true);
        while (mChatService.getState() != STATE_CONNECTED) {
            mChatService.connect(device, true);
            while (mChatService.getState() == STATE_CONNECTING) {
                // loop
            }
        }
    }

    public void fileManagement(String readMessage) {
        if (fileShareManagement == null) {
            try {
                fileShareManagement = new FileShareManagement(mBluetoothAdapter, mChatService, context);
            } catch (Exception e){}
        }
        fileShareManagement.fileShare(readMessage);
    }

    public void NodeAssignment(String readMessage) throws Exception {
        if (readMessage.startsWith("findHead")) {
            String splitArr[] = readMessage.split(";");
            DataBase dataBase = new DataBase(context);
            dataBase.open();

            String head = dataBase.getHead("head");

            String splitAddArr[] = dataBase.getHead("mine").split(";");

            String deviceAddress = "";
            if (splitAddArr.length > 2 )  {
                deviceAddress = splitAddArr[2].trim();
            }

            String headInfo[] = head.split(";");
            if (head.startsWith("headFound")) {
                if (!deviceAddress.equals(headInfo[2].trim())) {
                    // Get the Head BluetoothDevice object
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(headInfo[2].trim());
                    // Attempt to connect to the device
                    Toast.makeText(context, device.getAddress() ,Toast.LENGTH_LONG).show();
                    //mChatService.connectionLost();
                    //mChatService.start();
                    try {
                        //mChatService.stop();
                    } catch (Exception e) {
                        //
                    }
                    // @Sumit: connection establish with head
                    setUpDevice(device);

                    // Element sb = (Element ) stringToObject(splitAddArr[4].trim());
                    // Element puN = (Element ) stringToObject(splitArr[4].trim());
                    // String beta1 = objectToString(sb.duplicate().mul(puN));

                    String beta1 =  splitAddArr[4].trim() + "#" + splitArr[4].trim();

                    head = "sendHeadTo ; " + splitArr[1].trim() + " ; " + splitArr[2].trim()  + " ; " + splitArr[3].trim() + ";" + splitArr[4].trim() + ";" + beta1;
                }
            } else {
                head = "Couldn't find Head";
            }

            // Generate OverlayID and Send
            String overlayId = headInfo[3].trim().substring(0,20)+splitArr[3].trim().substring(20);
            head += " ; " + overlayId;

            byte[] send = head.getBytes();
            mChatService.write(send);

            dataBase.addNode(overlayId, splitArr[1].trim(), splitArr[2].trim(), splitArr[3].trim(), "", "" , "");
            dataBase.close();
            // Reset out string buffer to zero and clear the edit text field
//            mOutStringBuffer.setLength(0);
//            mOutEditText.setText(mOutStringBuffer);
        }
        if (readMessage.startsWith("sendHeadTo")) {
            String splitArr[] = readMessage.split(";");
            DataBase dataBase = new DataBase(context);
            dataBase.open();
            String splitAddArr[] = dataBase.getHead("mine").split(";");


            String overlayId = splitArr[6];
            // String overlayId = splitAddArr[3].trim().substring(0,20)+splitArr[3].trim().substring(20);

            // Element sv = (Element ) stringToObject(splitAddArr[4].trim());
            // Element beta1 = (Element ) stringToObject(splitArr[5].trim());
            // Element puN = (Element ) stringToObject(splitArr[4].trim());

            // String gamma1 = objectToString(sv.duplicate().mul(beta1));
            // String gamma2 = objectToString(sv.duplicate().mul(puN));

            String gamma1 = splitAddArr[4].trim() + "#" + splitArr[5].trim();
            String gamma2 = splitAddArr[4].trim() + "#" + splitArr[4].trim();

            String idN = hashSHA256(overlayId+splitArr[4].trim()+gamma1);
            String tow = ""+new Date();

            BLS01ParametersGenerator setup = new BLS01ParametersGenerator();
            setup.init(PairingFactory.getPairingParameters("assets/a.properties"));

            BLS01KeyPairGenerator keyGen = new BLS01KeyPairGenerator();
            keyGen.init(new BLS01KeyGenerationParameters(null, setup.generateParameters()));
            AsymmetricCipherKeyPair keyPair = keyGen.generateKeyPair();

            String signedToken = String.valueOf(sign(tow, keyPair.getPrivate()));

            String pubKey = keyPair.getPublic().toString();

            // reheadfound + 6 + 7 + 8 + 9 + 10
            String headFound = "Re"+dataBase.getHead("mine") + " ; " + overlayId + " ; " + splitArr[2].trim() + " ; "+ idN + " ; " + gamma2 + " ; " + tow;
            byte[] send = headFound.getBytes();
            mChatService.write(send);

            dataBase.addNode(overlayId, splitArr[1].trim(), splitArr[2].trim(), splitArr[3].trim(), "", "", "");
            dataBase.close();
            // Reset out string buffer to zero and clear the edit text field
//            mOutStringBuffer.setLength(0);
//            mOutEditText.setText(mOutStringBuffer);
        }
        if (readMessage.startsWith("ReheadFound")) {

            String msg  = "headRcvd";
            byte[] sendR = msg.getBytes();
            mChatService.write(sendR);

            String splitArr[] = readMessage.split(";");

            // Get the Node BluetoothDevice object

            // @send to same device.
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(splitArr[7].trim());
            setUpDevice(device);

            // CipherParameters pubKey = (CipherParameters) stringToObject(splitArr[11].trim());
            BLS01ParametersGenerator setup = new BLS01ParametersGenerator();
            setup.init(PairingFactory.getPairingParameters("assets/a.properties"));

            BLS01KeyPairGenerator keyGen = new BLS01KeyPairGenerator();
            keyGen.init(new BLS01KeyGenerationParameters(null, setup.generateParameters()));
            AsymmetricCipherKeyPair keyPair = keyGen.generateKeyPair();

            boolean status = verify(sign(splitArr[10].trim(), keyPair.getPrivate()), splitArr[10].trim() , keyPair.getPublic());

            String beta3 = "";
            if (status) {
                // Calculate
                DataBase dataBase = new DataBase(context);
                dataBase.open();
                String splitAddArr[] = dataBase.getHead("mine").split(";");
                dataBase.close();

                // Element sb = (Element ) stringToObject(splitAddArr[4].trim());
                // Element gamma2 = (Element ) stringToObject(splitArr[9].trim());
                // Element puV = (Element ) stringToObject(splitArr[11].trim());
                // String beta2 = objectToString(sb.duplicate().mul(gamma2));

                //beta3 = objectToString(sb.duplicate().mul(puV));
                String beta2 = splitArr[9].trim().split("#")[0]+ "#" + splitAddArr[4].trim() + "#" + splitArr[9].trim().split("#")[1];
                //beta3 = splitAddArr[4].trim() + "#" + splitArr[11].trim();

                if (hashSHA256(splitArr[6].trim()+ splitArr[9].trim().split("#")[1] + beta2) == splitArr[8]) {
                    // Success.
                }
            }

            String headFound = readMessage.substring(2) + " ; " + beta3;
            byte[] send = headFound.getBytes();
            mChatService.write(send);

            // Reset out string buffer to zero and clear the edit text field
//            mOutStringBuffer.setLength(0);
//            mOutEditText.setText(mOutStringBuffer);
        }
        if (readMessage.startsWith("headFound")) {
            String splitArr[] = readMessage.split(";");

            // Element sn = (Element ) stringToObject(splitArr[4].trim());
            // Element puV = (Element ) stringToObject(splitArr[11].trim());
            // String alpha1 = objectToString(sn.duplicate().mul(puV));

            // String alpha1 = objectToString(sn.duplicate().mul(puV));

            // if (hashSHA256(splitArr[6].trim()+ splitArr[13].trim() + alpha1) == splitArr[8]) {
                // Success.
            // }

            DataBase dataBase = new DataBase(context);
            dataBase.open();

            // Received OverlayId from Head
            // add head information
            dataBase.addNode(splitArr[3].trim(), splitArr[1].trim(), splitArr[2].trim(), splitArr[3].trim(), "head", "", "");

            // update my overlay ID.
            //dataBase.updateOverlayId(splitArr[6].trim());

            Toast.makeText(context, dataBase.updateOverlayId(splitArr[6].trim()), Toast.LENGTH_SHORT).show();
            dataBase.close();
        }
    }
}
