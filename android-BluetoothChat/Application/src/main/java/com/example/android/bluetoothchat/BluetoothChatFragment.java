package com.example.android.bluetoothchat;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import net.vidageek.mirror.dsl.Mirror;

import com.example.android.common.logger.Log;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.w3c.dom.Node;

import java.lang.reflect.Field;
import java.security.MessageDigest;

import it.unisa.dia.gas.crypto.jpbc.signature.bls01.engines.BLS01Signer;
import it.unisa.dia.gas.crypto.jpbc.signature.bls01.generators.BLS01KeyPairGenerator;
import it.unisa.dia.gas.crypto.jpbc.signature.bls01.generators.BLS01ParametersGenerator;
import it.unisa.dia.gas.crypto.jpbc.signature.bls01.params.BLS01KeyGenerationParameters;
import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;

import static com.example.android.bluetoothchat.BluetoothChatService.STATE_CONNECTED;
import static com.example.android.bluetoothchat.BluetoothChatService.STATE_CONNECTING;

/**
 * This fragment controls Bluetooth to communicate with other devices.
 */
public class BluetoothChatFragment extends Fragment {

    NodeManagement nodeManagement;

    public static boolean flagShareNSend = true;
    public static String sharedFileName = "";

    public static long timeForFindingHead, timeForSendingToHead, timeByHead;
    public static float engForFindingHead, engForSendingToHead, engByHead;

    private static final String TAG = "BluetoothChatFragment";

    private static int connectionState = 0;

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;

    // Layout Views
    private ListView mConversationView;
    private EditText mOutEditText, macAdd;
    private TextView editTextView;
    private Button mSendButton, mSendFileButton, shareFile, verify2, updateMacAdd, getFile, timeBtn, energyBtn;

    /**
     * Name of the connected device
     */
    private String mConnectedDeviceName = null;

    /**
     * Array adapter for the conversation thread
     */
    private ArrayAdapter<String> mConversationArrayAdapter;

    /**
     * String buffer for outgoing messages
     */
    private StringBuffer mOutStringBuffer;

    /**
     * Local Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;

    /**
     * Member object for the chat services
     */
    private BluetoothChatService mChatService = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            FragmentActivity activity = getActivity();
            Toast.makeText(activity, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            activity.finish();
        }
    }


    @Override
    public void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else if (mChatService == null) {
            setupChat();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mChatService != null) {
            mChatService.stop();
        }
    }

    @Override
    public void onResume() {

        if (sharedFileName != "") {
            mOutEditText.setText("shareFile;" + sharedFileName+ ";"+ NodeManagement.hashSHA256(sharedFileName));
        }

        super.onResume();

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
                // Start the Bluetooth chat services
                mChatService.start();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // @Sumit: Using Layout File
        return inflater.inflate(R.layout.fragment_bluetooth_chat, container, false);
    }


    public void testSign(){
        BLS01ParametersGenerator setup = new BLS01ParametersGenerator();
        setup.init(PairingFactory.getPairingParameters("assets/a.properties"));

        BLS01KeyPairGenerator keyGen = new BLS01KeyPairGenerator();
        keyGen.init(new BLS01KeyGenerationParameters(null, setup.generateParameters()));
        AsymmetricCipherKeyPair keyPair = keyGen.generateKeyPair();

        String testMessage = "Test Joining Nodes";
        verify(sign(testMessage, keyPair.getPrivate()), testMessage, keyPair.getPublic());

    }

    public void computeCrypto(){
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


        } catch(Exception e) {

        }


    }

    public void testNodes () {
        String s = "";
        try {
            if (nodeManagement == null) {
                String add = macAdd.getText().toString().trim();
                nodeManagement = new NodeManagement(add, mBluetoothAdapter, mChatService, getContext());
            }
            s = nodeManagement.generatePubPrivKey();
            //computeCrypto
            computeCrypto();
            // signature verification
            testSign();
        } catch(Exception e) {

        }

    }



    public void testNodesAtFriend () {
        String s = "";
        try {
            if (nodeManagement == null) {
                String add = macAdd.getText().toString().trim();
                nodeManagement = new NodeManagement(add, mBluetoothAdapter, mChatService, getContext());
            }
            s = nodeManagement.generatePubPrivKey();

            //computeCrypto
            computeCrypto();
            computeCrypto(); // 2nd time computation
            // signature verification
            testSign();

        } catch(Exception e) {

        }


    }

    public String testNodesAtMS () {
        String s = "";
        try {
            if (nodeManagement == null) {
                String add = macAdd.getText().toString().trim();
                nodeManagement = new NodeManagement(add, mBluetoothAdapter, mChatService, getContext());
            }
            s = nodeManagement.generatePubPrivKey();
            //computeCrypto at the end
            computeCrypto();
        } catch(Exception e) {

        }
        return s;
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


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

        mConversationView = (ListView) view.findViewById(R.id.in);
        mOutEditText = (EditText) view.findViewById(R.id.edit_text_out);
        mSendButton = (Button) view.findViewById(R.id.button_send);
        // mSendFileButton = (Button) view.findViewById(R.id.button_sendFile);
        shareFile = (Button) view.findViewById(R.id.button_shareFile);
        getFile = (Button) view.findViewById(R.id.button_get);

        timeBtn = (Button) view.findViewById(R.id.timeBtn);
        energyBtn = (Button) view.findViewById(R.id.energyBtn);

        editTextView = (TextView) view.findViewById(R.id.edit_text_out);

        // @Sumit: Import editText to take mac entry
        macAdd = (EditText) view.findViewById(R.id.macAdd);

//sumit- hash the macAdd and divive it into 3 parts (8+8+16= 32 bits).
//  send 16 bits to pilot through friend node. Pilot add 8+8 bits and return 32 bits to new node

        // @Sumit: Import that button from layout
        updateMacAdd = (Button) view.findViewById(R.id.updateMacAdd);

        // Retrieve My Mac address from datbase to show
        DataBase dataBase = new DataBase(getActivity());
        dataBase.open();
        String splitArr[] = dataBase.getHead("mine").split(";");
        String deviceAddress = "00:00:00:00:00:00";
        if (splitArr.length > 2 )  {
            deviceAddress = splitArr[2].trim();
        }
        dataBase.close();
        macAdd.setText(deviceAddress);

        // Test Code Start
        try {
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = getContext().registerReceiver(null, ifilter);

            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

            float batteryPct = level / (float)scale;

            Toast.makeText(getActivity(), "Battery: "+ batteryPct, Toast.LENGTH_LONG).show();

            BLS01ParametersGenerator setup = new BLS01ParametersGenerator();
            setup.init(PairingFactory.getPairingParameters("assets/a.properties"));

            BLS01KeyPairGenerator keyGen = new BLS01KeyPairGenerator();
            keyGen.init(new BLS01KeyGenerationParameters(null, setup.generateParameters()));
            AsymmetricCipherKeyPair keyPair = keyGen.generateKeyPair();

            String message = "Hello World!";
        } catch(Exception e) {
            Toast.makeText(getActivity(), e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Set up the UI and background operations for chat.
     */
    private void setupChat() {
        Log.d(TAG, "setupChat()");

        // Initialize the array adapter for the conversation thread
        mConversationArrayAdapter = new ArrayAdapter<String>(getActivity(), R.layout.message);

        mConversationView.setAdapter(mConversationArrayAdapter);

        // Initialize the compose field with a listener for the return key
        mOutEditText.setOnEditorActionListener(mWriteListener);

        // @Sumit: Add Mac Entry When Clicked
        updateMacAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String add = macAdd.getText().toString().trim();
                try {
                    nodeManagement = new NodeManagement(add, mBluetoothAdapter, mChatService, getContext());
                } catch (Exception e){}
            }
        });

        shareFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                flagShareNSend = false;
                //if (sharedFileName == "") {
                    // Send a message using content of the edit text widget
                    Intent intent = new Intent(getActivity(), ListFileActivity.class);
                    startActivity(intent);
                //} else {
                //     sendMessage("shareFile;"+sharedFileName+";"+ NodeManagement.hashSHA256(sharedFileName));
                //    sharedFileName = "";
                //}
            }
        });

        getFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mChatService.getState() != STATE_CONNECTED) {
                    Toast.makeText(getActivity(), R.string.not_connected, Toast.LENGTH_SHORT).show();
                    return;
                }

                String message = editTextView.getText().toString();

                // Check that there's actually something to send
                if (message.length() > 0) {
                    byte[] send = ("getFile:"+message).getBytes();
                    mChatService.write(send);

                    // Reset out string buffer to zero and clear the edit text field
                    mOutStringBuffer.setLength(0);
                    mOutEditText.setText(mOutStringBuffer);
                }
            }
        });

        // Initialize the send button with a listener that for click events
        mSendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                View view = getView();
                if (null != view) {
                    TextView textView = (TextView) view.findViewById(R.id.edit_text_out);
                    String message = textView.getText().toString();
                    /*for (int i = 0; i < 1000; i++){
                        String message = "TestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataTestdataReceived";
                        sendMessage(message);
                    }*/
                    sendMessage(message);
                }
            }
        });

        // Initialize the send button with a listener that for click events
        /*mSendFileButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                flagShareNSend = true;

                // Send a message using content of the edit text widget
                Intent intent = new Intent(getActivity(), ListFileActivity.class);
                startActivity(intent);
            }
        });*/

        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(getActivity(), mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }

    /**
     * Makes this device discoverable for 300 seconds (5 minutes).
     */
    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    /**
     * Sends a message.
     *
     * @param message A string of text to send.
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != STATE_CONNECTED) {
            Toast.makeText(getActivity(), R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {

            if (message.toLowerCase().startsWith("sharefile")) {
                sharedFileName = "";
            }

            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mChatService.write(send);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            mOutEditText.setText(mOutStringBuffer);
        }
    }

    /**
     * The action listener for the EditText widget, to listen for the return key
     */
    private TextView.OnEditorActionListener mWriteListener
            = new TextView.OnEditorActionListener() {
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
            // If the action is a key-up event on the return key, send the message
            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                String message = view.getText().toString();
                sendMessage(message);
            }
            return true;
        }
    };

    /**
     * Updates the status on the action bar.
     *
     * @param resId a string resource ID
     */
    private void setStatus(int resId) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(resId);
    }

    /**
     * Updates the status on the action bar.
     *
     * @param subTitle status
     */
    private void setStatus(CharSequence subTitle) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(subTitle);
    }

    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            FragmentActivity activity = getActivity();
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case STATE_CONNECTED:
                            connectionState = 1;
                            setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                            mConversationArrayAdapter.clear();
                            break;
                        case STATE_CONNECTING:
                            connectionState = 1;
                            setStatus(R.string.title_connecting);
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            connectionState = 0;
                            setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    mConversationArrayAdapter.add("Me:  " + writeMessage);
                    break;
                // @Sumit: Handle Find Head
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    //mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + readMessage);

                    try {
                        if (nodeManagement == null) {
                            String add = macAdd.getText().toString().trim();
                            nodeManagement = new NodeManagement(add, mBluetoothAdapter, mChatService, getContext());
                        }
                        if (readMessage.startsWith("findHead") || readMessage.startsWith("sendHeadTo") || readMessage.startsWith("headFound") || readMessage.startsWith("ReheadFound") || readMessage.startsWith("headRcvd")) {// According To ReadMessage
                            if (readMessage.startsWith("headFound")) {
                                timeForFindingHead = System.currentTimeMillis() - timeForFindingHead;
                                engForFindingHead = NodeManagement.getEnergy(getContext()) - engForFindingHead;
                                energyBtn.setText("E: "+ engForFindingHead);
                                timeBtn.setText("T: "+ timeForFindingHead);

                                // Print only in case of HeadFound;
                                String splitArr[] = readMessage.split(";");
                                mConversationArrayAdapter.clear();
                                String parsedMessage = "Assigned Node Id is - " + splitArr[6].trim();
                                mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + parsedMessage);
                            }

                            if (readMessage.startsWith("sendHeadTo")) {
                                timeByHead = System.currentTimeMillis();
                                engByHead = NodeManagement.getEnergy(getContext());
                            }

                            if (readMessage.startsWith("headRcvd")) {
                                timeByHead = System.currentTimeMillis() - timeByHead;
                                engByHead = NodeManagement.getEnergy(getContext()) - engByHead;
                                energyBtn.setText("E: "+ engByHead);
                                timeBtn.setText("T: "+ timeByHead);
                            }

                            if (readMessage.startsWith("findHead")) {
                                timeForSendingToHead = System.currentTimeMillis();
                                engForSendingToHead = NodeManagement.getEnergy(getContext());
                            }

                            if (readMessage.startsWith("ReheadFound")) {
                                timeForSendingToHead = System.currentTimeMillis() - timeForSendingToHead;
                                engForSendingToHead = NodeManagement.getEnergy(getContext()) - engForSendingToHead;
                                energyBtn.setText("E: "+ engForSendingToHead);
                                timeBtn.setText("T: "+ timeForSendingToHead);
                            }
                            nodeManagement.NodeAssignment(readMessage);
                        }
                        if (readMessage.startsWith("shareFile") || readMessage.startsWith("getFile:") ) {
                            // According To ReadMessage
                            nodeManagement.fileManagement(readMessage);
                            mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + readMessage);
                        }
                        else {
                            mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + readMessage);
                        }
                    } catch (Exception e) {
                        Toast.makeText(getActivity(), e.toString(), Toast.LENGTH_SHORT).show();
                    }

                    mOutStringBuffer.setLength(0);
                    mOutEditText.setText(mOutStringBuffer);
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    connectionState = 1;
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    if (null != activity) {
                        Toast.makeText(activity, "Connected to "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Constants.MESSAGE_TOAST:
                    if (null != activity) {
                        Toast.makeText(activity, msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, true);
                }
                break;
            case REQUEST_CONNECT_DEVICE_INSECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, false);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupChat();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(getActivity(), R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                }
        }
    }

    /**
     * Establish connection with other device
     *
     * @param data   An {@link Intent} with {@link DeviceListActivity#EXTRA_DEVICE_ADDRESS} extra.
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras()
                .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        Toast.makeText(getActivity(), device.getAddress(),
                Toast.LENGTH_SHORT).show();
        // Attempt to connect to the device
        //mChatService.start();
        //mChatService.connectionLost();
        try {
            if (nodeManagement == null) {
                String add = macAdd.getText().toString().trim();
                nodeManagement = new NodeManagement(add, mBluetoothAdapter, mChatService, getContext());
            }
            nodeManagement.setUpDevice(device);
        } catch (Exception e) {
            Toast.makeText(getActivity(), e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.bluetooth_chat, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.secure_connect_scan: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
                return true;
            }
            case R.id.insecure_connect_scan: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
                return true;
            }
            case R.id.discoverable: {
                // Ensure this device is discoverable by others
                ensureDiscoverable();
                return true;
            }

            case R.id.sendFile: {
                flagShareNSend = true;

                // Send a message using content of the edit text widget
                Intent intent = new Intent(getActivity(), ListFileActivity.class);
                startActivity(intent);
                return true;
            }

            case R.id.testNodes: {
                long time = System.currentTimeMillis();
                float energy = NodeManagement.getEnergy(getContext());
                for (int i = 0; i < 300 ; i++) {
                    testNodes();
                    mConversationArrayAdapter.add("Test Node " + (i + 1) + "joined");
                }
                time = System.currentTimeMillis() - time;
                energy = NodeManagement.getEnergy(getContext()) - energy;
                energyBtn.setText("E: "+ energy);
                timeBtn.setText("T: "+ time);
                return true;
            }

            case R.id.testAtFriend: {
                long time = System.currentTimeMillis();
                float energy = NodeManagement.getEnergy(getContext());
                for (int i = 0; i < 300 ; i++) {
                    testNodesAtFriend();
                    mConversationArrayAdapter.add("Test Node " + (i + 1) + "joined");
                }
                time = System.currentTimeMillis() - time;
                energy = NodeManagement.getEnergy(getContext()) - energy;
                energyBtn.setText("E: "+ energy);
                timeBtn.setText("T: "+ time);
                return true;
            }

            case R.id.testAtMS: {
                long time = System.currentTimeMillis();
                float energy = NodeManagement.getEnergy(getContext());
                String genID;
                int totalIDs=0;
                for (int i = 0; i < 300 ; i++) {
                    genID= testNodesAtMS();
                    totalIDs=i;
                }
                mConversationArrayAdapter.add("Number of Generated ID " + totalIDs + "joined");
                time = System.currentTimeMillis() - time;
                energy = NodeManagement.getEnergy(getContext()) - energy;
                energyBtn.setText("E: "+ energy);
                timeBtn.setText("T: "+ time);
                return true;
            }
            // @Sumit: Make Me Head
            case R.id.makeMeHead: {
                String deviceName = mBluetoothAdapter.getName();
                DataBase dataBase = new DataBase(getActivity());
                dataBase.open();
                String splitArr[] = dataBase.getHead("mine").split(";");
                String deviceAddress = "";

                if (splitArr.length > 2 )  {
                    deviceAddress = splitArr[2].trim();
                }
                if (deviceAddress.length() == 17) {
                    try {
                        NodeManagement headNodeManagement = new NodeManagement(deviceAddress, mBluetoothAdapter, mChatService, getContext(), "head");
                    } catch (Exception e) {
                        //
                    }
                } else {
                    //setUpDevice(device);
                }
                return true;
            }

            // @Sumit: Get Head Information from Head or other nodes in network
            case R.id.findHead: {
                DataBase dataBase = new DataBase(getActivity());
                dataBase.open();
                String splitArr[] = dataBase.getHead("mine").split(";");
                String deviceAddress = "";
                if (splitArr.length > 2 )  {
                    deviceAddress = splitArr[2].trim();
                }
                dataBase.close();
                if (deviceAddress.length() == 17) {
                    timeForFindingHead = System.currentTimeMillis();
                    engForFindingHead = NodeManagement.getEnergy(getContext());
                    sendMessage("findHead;"+mBluetoothAdapter.getName()+";"+deviceAddress+";"+splitArr[3].trim()+";"+splitArr[5].trim());
                } else {
                    //setUpDevice();
                }
                return true;
            }
            // @Sumit: Show All Nodes
            case R.id.showAllNodes: {
                DataBase dataBase = new DataBase(getActivity());
                dataBase.open();
                String nodes = dataBase.showAllNodes();
                dataBase.close();
                Toast.makeText(getActivity(), nodes, Toast.LENGTH_LONG).show();
                return true;
            }
        }
        return false;
    }
}
