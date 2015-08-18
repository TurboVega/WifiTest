package com.test.curtis.wifitest;

import android.os.Handler;
import android.os.SystemClock;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;


public class MainActivity extends ActionBarActivity {

    TextView statusText;
    InetAddress serverAddr;
    Socket socket;
    Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        statusText = (TextView) findViewById(R.id.status_text);
        handler = new Handler();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        showStatusText("Starting comm thread...\n");
        CommThread commThread = new CommThread();
        commThread.start();
        showStatusText("Comm thread started.\n");
    }
    
    private void showStatusText(String text) {
        statusText.append(text);
        Log.d("WifiTest", text);
    }

    private class StatusUpdater implements Runnable {

        private String text;

        public StatusUpdater(String text) {
            this.text = new String(text);
        }

        @Override
        public void run() {
            showStatusText(text);
        }
    }

    private void showStatusTextFromThread(String text) {
        handler.post(new StatusUpdater(text));
    }

    private class CommThread extends Thread {
        @Override
        public void run() {
            try {
                MainActivity.this.showStatusTextFromThread("Connecting...\n");
                serverAddr = InetAddress.getByName("10.10.100.100");
                if (serverAddr == null) {
                    MainActivity.this.showStatusTextFromThread("Can't create server address!\n");
                    return;
                }
                socket = new Socket(serverAddr, 8899);
                if (!socket.isConnected()) {
                    MainActivity.this.showStatusTextFromThread("Can't connect to Robot!\n");
                    return;
                }
                MainActivity.this.showStatusTextFromThread("Connected to Robot.\n");
                PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                long lastWrite = SystemClock.elapsedRealtime();
                int count = 0;
                char ch = 'A';
                while (true) {
                    long now = SystemClock.elapsedRealtime();
                    if (now - lastWrite >= 3000) {
                        if (count >= 5)
                            break;
                        MainActivity.this.showStatusTextFromThread("(" + Long.toString(now) + ") SND: " + Character.toString(ch) + "\n");
                        out.println(ch++);
                        count++;
                        lastWrite = now;
                    }
                    if (in.ready()) {
                        char ch2 = (char) in.read();
                        MainActivity.this.showStatusTextFromThread("(" + Long.toString(now) + ") RCV: " + Character.toString(ch2) + "\n");
                    } else {
                        Thread.sleep(100);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                MainActivity.this.showStatusTextFromThread(e.getLocalizedMessage());
            } finally {
                try {
                    if (socket != null)
                        socket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    MainActivity.this.showStatusTextFromThread(e.getLocalizedMessage());
                }
            }
        }
    }
}
