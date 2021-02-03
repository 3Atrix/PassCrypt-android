package com.passcrypt.android;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.text.method.TransformationMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import A1.A1;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, View.OnFocusChangeListener, ActivityCompat.OnRequestPermissionsResultCallback {

    private String backupText = "";
    private boolean isCrypting = false;
    //////
    private final String homePath = Environment.getExternalStorageDirectory().getAbsolutePath();
    private String cipherTxtName;
    private String cipherTxtPath;
    private boolean cipherTxtFlag = false;
    //////
    private String plainDirName;
    private String cipherDirName;
    private String plainDirPath;
    private String cipherDirPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        plainDirName = getResources().getString(R.string.plain_dir_name);
        cipherDirName = getResources().getString(R.string.cipher_dir_name);
        plainDirPath = homePath + "/" + plainDirName;
        cipherDirPath = homePath + "/" + cipherDirName;

        cipherTxtName = getResources().getString(R.string.cipher_file_name);
        cipherTxtPath = homePath + "/" + cipherTxtName;

        initView();
        requestPermissions();
    }

    TextView tv_show_msg;
    EditText et_input_text;
    EditText et_pass_1;
    EditText et_pass_2;
    EditText et_pass_3;
    Switch switch_show_pass;
    Switch switch_show_text;

    private void initView() {
        tv_show_msg = this.findViewById(R.id.tv_msg);
        et_input_text = this.findViewById(R.id.et_input_text);

        et_pass_1 = this.findViewById(R.id.et_pass_1);
        et_pass_2 = this.findViewById(R.id.et_pass_2);
        et_pass_3 = this.findViewById(R.id.et_pass_3);

        switch_show_pass = this.findViewById(R.id.switch_show_pass);
        switch_show_text = this.findViewById(R.id.switch_show_text);

        //////

        et_pass_1.setOnFocusChangeListener(this);
        et_pass_2.setOnFocusChangeListener(this);
        et_pass_3.setOnFocusChangeListener(this);

        switch_show_pass.setOnClickListener(this);
        switch_show_text.setOnClickListener(this);

        Button btn = this.findViewById(R.id.btn_encrypt);
        btn.setOnClickListener(this);
        btn = this.findViewById(R.id.btn_encrypt_file);
        btn.setOnClickListener(this);
        btn = this.findViewById(R.id.btn_decrypt);
        btn.setOnClickListener(this);
        btn = this.findViewById(R.id.btn_decrypt_file);
        btn.setOnClickListener(this);
        btn = this.findViewById(R.id.btn_exit);
        btn.setOnClickListener(this);
        btn = this.findViewById(R.id.btn_recovery);
        btn.setVisibility(View.GONE);
//        btn.setOnClickListener(this); //< reboot recovery : Permission Denied; Need su or disable selinux
        btn = this.findViewById(R.id.btn_delete_cache);
        btn.setOnClickListener(this);
    }

    private void setShowPass(boolean isShow) {
        TransformationMethod tm;

        if (isShow) {
            tm = HideReturnsTransformationMethod.getInstance();
        } else {
            tm = PasswordTransformationMethod.getInstance();
        }
        et_pass_1.setTransformationMethod(tm);
        et_pass_2.setTransformationMethod(tm);
        et_pass_3.setTransformationMethod(tm);
    }

    private void setShowText(boolean isShow) {
        if (isShow) {
            et_input_text.setEnabled(true);
            et_input_text.setText(backupText);
            backupText = "";
        } else {
            backupText = et_input_text.getText().toString();
            et_input_text.setText(R.string.view_text);
            et_input_text.setEnabled(false);
        }
    }

    private void encryptString() {
        if (isCrypting) {
            return;
        }

        if (!checkPassValid()) {
            return;
        }

        String text;
        if (switch_show_text.isChecked()) {
            text = et_input_text.getText().toString();
        } else {
            text = backupText;
        }
        if (text.isEmpty()) {
            tv_show_msg.setText(R.string.inputed_text_empty);
            return;
        }

        isCrypting = true;

        et_input_text.getText().clear();
        et_input_text.setEnabled(false);
        switch_show_text.setChecked(false);
        switch_show_text.setEnabled(false);
        tv_show_msg.setText(R.string.encrypting);
        final String plainText = text;
        TaskManager.runOnThread(new Runnable() {
            @Override
            public void run() {
                new File(cipherTxtPath).delete();
                cipherTxtFlag = false;

                backupText = A1.a3_encrypt_str(plainText, et_pass_1.getText().toString(), et_pass_2.getText().toString(), et_pass_3.getText().toString());
                if (!backupText.isEmpty()) {
                    cipherTxtFlag = A1.fileWriteFile(cipherTxtPath, backupText.getBytes());
                }
                TaskManager.runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        tv_show_msg.setText(R.string.encrypt_success);
                        switch_show_text.setEnabled(true);

                        String input_text = getResources().getString(R.string.view_ciphertext);
                        if (cipherTxtFlag) {
                            input_text += "\n" + getResources().getString(R.string.cipher_content_is_saved_in_file) + " /sdcard/" + cipherTxtName;
                        }
                        et_input_text.setText(input_text);

                        isCrypting = false;
                    }
                });
            }
        });
    }

    private void decryptString() {
        if (isCrypting) {
            return;
        }

        if (!checkPassValid()) {
            return;
        }

        String text;
        if (switch_show_text.isChecked()) {
            text = et_input_text.getText().toString();
        } else {
            text = backupText;
        }

        cipherTxtFlag = false;
        if (text.isEmpty()) {
            byte[] bytes = A1.fileReadFile(cipherTxtPath);
            if (null == bytes || 0 == bytes.length) {
                String input_txt = getResources().getString(R.string.inputed_text_empty);
                input_txt += "\n" + getResources().getString(R.string.put_ciphertext_file_in_path) + " /sdcard/" + cipherTxtName;
                tv_show_msg.setText(input_txt);
                return;
            }

            cipherTxtFlag = true;
            text = new String(bytes);
        }

        isCrypting = true;

        et_input_text.getText().clear();
        et_input_text.setEnabled(false);
        switch_show_text.setChecked(false);
        switch_show_text.setEnabled(false);
        tv_show_msg.setText(R.string.decrypting);
        final String cipherText = text;
        TaskManager.runOnThread(new Runnable() {
            @Override
            public void run() {
                backupText = A1.a3_decrypt_str(cipherText, et_pass_1.getText().toString(), et_pass_2.getText().toString(), et_pass_3.getText().toString());
                TaskManager.runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        tv_show_msg.setText(R.string.decrypt_success);
                        switch_show_text.setEnabled(true);

                        String input_txt = getResources().getString(R.string.view_plaintext);
                        if (cipherTxtFlag) {
                            input_txt += "\n" + getResources().getString(R.string.plaintext_decrypted_from_file) + " /sdcard/" + cipherTxtName;
                        }
                        et_input_text.setText(input_txt);

                        isCrypting = false;
                    }
                });
            }
        });
    }

    private void encryptFiles() {
        if (isCrypting) {
            return;
        }

        if (!checkPassValid()) {
            return;
        }

        if (!new File(plainDirPath).exists()) {
            String text = getResources().getString(R.string.put_plain_file_in_dir) + " /sdcard/" + plainDirName;
            tv_show_msg.setText(text);
            return;
        }

        isCrypting = true;

        tv_show_msg.setText(R.string.file_encrypting);
        TaskManager.runOnThread(new Runnable() {
            @Override
            public void run() {
                final int count = A1.a3_encrypt_dir_to_dir(plainDirPath, et_pass_1.getText().toString(), et_pass_2.getText().toString(), et_pass_3.getText().toString(), cipherDirPath);
                TaskManager.runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        String text;
                        if (0 < count) {
                            text = getResources().getString(R.string.encrypt_success) + "\n" +
                                    getResources().getString(R.string.encrypted_file_in_dir) + " /sdcard/" + cipherDirName;
                        } else {
                            text = getResources().getString(R.string.put_plain_file_in_dir) + " /sdcard/" + plainDirName;
                        }
                        tv_show_msg.setText(text);

                        isCrypting = false;
                    }
                });
            }
        });
    }

    private void decryptFiles() {
        if (isCrypting) {
            return;
        }

        if (!checkPassValid()) {
            return;
        }
        if (!new File(cipherDirPath).exists()) {
            String text = getResources().getString(R.string.put_encrypted_file_in_dir) + " /sdcard/" + cipherDirName;
            tv_show_msg.setText(text);
            return;
        }

        isCrypting = true;

        tv_show_msg.setText(R.string.file_decrypting);
        TaskManager.runOnThread(new Runnable() {
            @Override
            public void run() {
                final int count = A1.a3_decrypt_dir_to_dir(cipherDirPath, et_pass_1.getText().toString(), et_pass_2.getText().toString(), et_pass_3.getText().toString(), plainDirPath);
                TaskManager.runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        String text;
                        if (0 < count) {
                            text = getResources().getString(R.string.decrypt_success) + "\n" +
                                    getResources().getString(R.string.plain_file_in_dir) + " /sdcard/" + plainDirName;
                        } else {
                            text = getResources().getString(R.string.put_encrypted_file_in_dir) + " /sdcard/" + cipherDirName;
                        }
                        tv_show_msg.setText(text);

                        isCrypting = false;
                    }
                });
            }
        });
    }

    private boolean checkPassValid() {
        boolean isGood;
        isGood = !et_pass_1.getText().toString().isEmpty();
        isGood |= !et_pass_2.getText().toString().isEmpty();
        isGood |= !et_pass_3.getText().toString().isEmpty();

        if (!isGood) {
            tv_show_msg.setText(R.string.one_pass_at_lease);
        }

        return isGood;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.switch_show_pass: {
                setShowPass(switch_show_pass.isChecked());
            }
            break;
            case R.id.switch_show_text: {
                setShowText(switch_show_text.isChecked());
            }
            break;
            case R.id.btn_encrypt: {
                encryptString();
            }
            break;
            case R.id.btn_encrypt_file: {
                encryptFiles();
            }
            break;
            case R.id.btn_decrypt: {
                decryptString();
            }
            break;
            case R.id.btn_decrypt_file: {
                decryptFiles();
            }
            break;
            case R.id.btn_exit: {
                exit();
            }
            break;
            case R.id.btn_recovery: {
                recovery();
            }
            break;
            case R.id.btn_delete_cache: {
                initEnv();
            }
            break;
        }
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        int id = v.getId();
        if (R.id.et_pass_1 != id && R.id.et_pass_2 != id && R.id.et_pass_3 != id) {
            return;
        }
        if (!hasFocus) {
            return;
        }

        if (switch_show_text.isChecked()) {
            if (et_input_text.getText().toString().isEmpty()) {
                return;
            }

            switch_show_text.setChecked(false);
            setShowText(false);
        }
    }

    private static final int PERMISSION_REQUEST_READ_WRITE_EXTERNAL_STORAGE = 1;

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_READ_WRITE_EXTERNAL_STORAGE) {
            do {
                if (2 != grantResults.length) {
                    break;
                }

                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    break;
                }

                if (grantResults[1] != PackageManager.PERMISSION_GRANTED) {
                    break;
                }

                return;
            } while (false);

            requestPermissions();
        }
    }

    private void requestPermissions() {
        // Permission has not been granted and must be requested.
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) ||
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            requestStoragePermissions();
        } else {
            requestStoragePermissions();
        }
    }

    private void requestStoragePermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                PERMISSION_REQUEST_READ_WRITE_EXTERNAL_STORAGE);
    }

    private void initEnv() {
        isCrypting = true;
        tv_show_msg.setText(R.string.erasing_files_waiting);

        TaskManager.runOnThread(new Runnable() {
            @Override
            public void run() {
                List<Thread> threadList = new ArrayList<>();

                zeroDir(cipherTxtPath, threadList);
                zeroDir(plainDirPath, threadList);
                zeroDir(cipherDirPath, threadList);

                for (Thread t : threadList) {
                    try {
                        t.join();
                    } catch (Exception e){}
                }

                new File(cipherTxtPath).delete();
                new File(plainDirPath).mkdirs();
                new File(cipherDirPath).mkdirs();

                A1.util_clear_dir(plainDirPath);
                A1.util_clear_dir(cipherDirPath);

                TaskManager.runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        tv_show_msg.setText(R.string.input_text_for_cryption);

                        isCrypting = false;
                    }
                });
            }
        });
    }

    private void exit() {
        this.finish();
        System.exit(0);
    }

    private void recovery() {
        AlertDialog.Builder updateBuilder = new AlertDialog.Builder(this);

        updateBuilder.setTitle(getResources().getString(R.string.recovery));
        updateBuilder.setCancelable(false);
        updateBuilder.setMessage(getResources().getString(R.string.confirm_reboot_recovery));
        updateBuilder.setPositiveButton(getResources().getString(R.string.confirm_yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
//                ShellUtil.execCommand("reboot recovery", false);
//                ShellUtil.execCommand("reboot", false);
            }
        });
        updateBuilder.setNegativeButton(getResources().getString(R.string.confirm_no), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        updateBuilder.show();
    }

    private void zeroDir(String path, List<Thread> threadList) {
        File file = new File(path);
        if (!file.exists()) {
            return;
        }

        if (file.isFile()) {
            zeroFile(file, threadList);
            return;
        }

        if (file.isDirectory()) {
            zeroDir(file, threadList);
        }
    }

    private void zeroDir(File parentFile, List<Thread> threadList) {
        File[] files = parentFile.listFiles();
        if (null == files) {
            return;
        }

        for (File file : files) {
            if (file.isFile()) {
                zeroFile(file, threadList);
            }
            else if (file.isDirectory()){
                zeroDir(file, threadList);
            }
        }
    }

    private final static int ERASE_COUNT = 5;

    private void zeroFile(final File file, List<Thread> threadList) {
        if (!file.exists()) {
            return;
        }

        if (0 >= file.length()) {
            return;
        }

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] zeroBytes = new byte[]{0, 0, 0, 0, 0, 0, 0, 0};
                byte[] endBytes = null;
                long endLen = file.length() % zeroBytes.length;
                if (0 < endLen) {
                    endBytes = new byte[(int) endLen];
                }

                for (int i = 0; i < ERASE_COUNT; i++) {
                    try {
                        RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");

                        MappedByteBuffer mappedByteBuffer = randomAccessFile.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, file.length());
                        for (long j = 0; j < (file.length() / zeroBytes.length); j++) {
                            mappedByteBuffer.put(zeroBytes);
                        }
                        if (null != endBytes) {
                            mappedByteBuffer.put(endBytes);
                        }
                        mappedByteBuffer.force();

                        randomAccessFile.close();
                    } catch (Exception e) {
                        Log.e("zeroFile", "" + e.getMessage());
                    }
                }
            }
        });

        threadList.add(t);
        t.start();
    }
}
