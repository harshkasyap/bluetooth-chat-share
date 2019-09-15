package com.example.android.bluetoothchat;

import android.support.v4.content.FileProvider;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.net.Uri;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by root on 25/1/16.
 */
public class ListFileActivity extends ListActivity {

    public final static String EXTRA_FILE_PATH = "file_path";
    public final static String EXTRA_SHOW_HIDDEN_FILES = "show_hidden_files";
    public final static String EXTRA_ACCEPTED_FILE_EXTENSIONS = "accepted_file_extensions";
    private final static String DEFAULT_INITIAL_DIRECTORY = "/";

    protected File Directory;
    protected ArrayList<File> Files;
    protected ListFileActivityListAdapter Adapter;
    protected boolean ShowHiddenFiles = false;
    protected String[] acceptedFileExtensions;
    int impFlag;
    AlertDialog alertDialog;
    static String dirName;
    EditText editText[];

    int month,year1,day;
    TextView tview;
    static final int DATE_DIALOG_ID=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LayoutInflater inflator = (LayoutInflater)
                getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View emptyView = inflator.inflate(R.layout.file_browse, null);
        ((ViewGroup) getListView().getParent()).addView(emptyView);
        getListView().setEmptyView(emptyView);

        // Directory = new File(DEFAULT_INITIAL_DIRECTORY);
        Directory = new File(Environment.getExternalStorageDirectory().getAbsolutePath());
        Files = new ArrayList<File>();

        Adapter = new ListFileActivityListAdapter(this, Files);
        setListAdapter(Adapter);

        acceptedFileExtensions = new String[] {};

        if(getIntent().hasExtra(EXTRA_FILE_PATH))
            Directory = new File(getIntent().getStringExtra(EXTRA_FILE_PATH));

        if(getIntent().hasExtra(EXTRA_SHOW_HIDDEN_FILES))
            ShowHiddenFiles = getIntent().getBooleanExtra(EXTRA_SHOW_HIDDEN_FILES, false);

        if(getIntent().hasExtra(EXTRA_ACCEPTED_FILE_EXTENSIONS)) {

            ArrayList<String> collection =
                    getIntent().getStringArrayListExtra(EXTRA_ACCEPTED_FILE_EXTENSIONS);

            acceptedFileExtensions = (String[])
                    collection.toArray(new String[collection.size()]);
        }

        getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                final File newFile = (File)parent.getItemAtPosition(position);
                return true;
            }
        });

    }

    @Override
    protected void onResume() {
        refreshFilesList();
        super.onResume();
    }

    protected void refreshFilesList() {

        Files.clear();
        ExtensionFilenameFilter filter =
                new ExtensionFilenameFilter(acceptedFileExtensions);

        File[] files = Directory.listFiles(filter);

        if(files != null && files.length > 0) {

            for(File f : files) {

                if(f.isHidden() && !ShowHiddenFiles) {

                    continue;
                }

                Files.add(f);
            }

            Collections.sort(Files, new FileComparator());
        }

        Adapter.notifyDataSetChanged();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {

        File newFile = (File)l.getItemAtPosition(position);

        if (newFile.isFile()) {

            if (!BluetoothChatFragment.flagShareNSend) {
                BluetoothChatFragment.sharedFileName = newFile.getName();
                super.onBackPressed();
                return;
            }

            try {
                //String path = newFile.getAbsolutePath();

                Intent i = new Intent();
                i.setAction(Intent.ACTION_SEND);
                i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                //i.putExtra(Intent.EXTRA_STREAM, "content://"+Uri.parse(newFile.getAbsolutePath()).toString());
                //Toast.makeText(this, "content://"+Uri.parse(newFile.getAbsolutePath()).toString(), Toast.LENGTH_LONG).show();
                //Uri uri = FileProvider.getUriForFile(getApplicationContext(),"com.example.android.bluetoothchat.ListFileActivity", newFile);
                Uri uri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID, newFile);
                i.setDataAndType(uri, "*/*");
                i.putExtra(Intent.EXTRA_STREAM, uri);
                PackageManager pm = getPackageManager();
                List<ResolveInfo> list = pm.queryIntentActivities(i, 0);
                if (list.size() > 0) {
                    String packageName = null;
                    String className = null;
                    boolean found = false;

                    for (ResolveInfo info : list) {
                        packageName = info.activityInfo.packageName;
                        if (packageName.equals("com.android.bluetooth")) {
                            className = info.activityInfo.name;
                            found = true;
                            break;
                        }
                    }
                    //CHECK BLUETOOTH available or not------------------------------------------------
                    if (!found) {
                        Toast.makeText(this, "Bluetooth not been found", Toast.LENGTH_LONG).show();
                    } else {
                        i.setClassName(packageName, className);
                        startActivity(i);
                    }
                }
            } catch (Exception e) {
                Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
            }
        }
        else {
            Directory = newFile;
            refreshFilesList();
        }

        super.onListItemClick(l, v, position, id);
    }

    private class ListFileActivityListAdapter extends ArrayAdapter<File> {

        private List<File> mObjects;

        public ListFileActivityListAdapter(Context context, List<File> objects) {

            super(context, R.layout.activity_list_files, android.R.id.text1, objects);
            mObjects = objects;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            View row = null;

            if(convertView == null) {

                LayoutInflater inflater = (LayoutInflater)
                        getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                row = inflater.inflate(R.layout.activity_list_files, parent, false);
            }
            else
                row = convertView;

            File object = mObjects.get(position);

            ImageView imageView = (ImageView)row.findViewById(R.id.file_picker_image);
            TextView textView = (TextView)row.findViewById(R.id.file_picker_text);
            textView.setSingleLine(true);
            textView.setText(object.getName());

            if(object.isFile())
                imageView.setImageResource(R.drawable.file);

            else
                imageView.setImageResource(R.drawable.folder);

            return row;
        }
    }

    private class FileComparator implements Comparator<File> {

        public int compare(File f1, File f2) {

            if(f1 == f2)
                return 0;

            if(f1.isDirectory() && f2.isFile())
                // Show directories above files
                return -1;

            if(f1.isFile() && f2.isDirectory())
                // Show files below directories
                return 1;

            // Sort the directories alphabetically
            return f1.getName().compareToIgnoreCase(f2.getName());
        }
    }

    private class ExtensionFilenameFilter implements FilenameFilter {

        private String[] Extensions;

        public ExtensionFilenameFilter(String[] extensions) {

            super();
            Extensions = extensions;
        }

        public boolean accept(File dir, String filename) {

            if(new File(dir, filename).isDirectory()) {

                // Accept all directory names
                return true;
            }

            if(Extensions != null && Extensions.length > 0) {

                for(int i = 0; i < Extensions.length; i++) {

                    if(filename.endsWith(Extensions[i])) {

                        // The filename ends with the extension
                        return true;
                    }
                }
                // The filename did not match any of the extensions
                return false;
            }
            // No extensions has been set. Accept all file extensions.
            return true;
        }
    }
}


