package com.futurice.rxandroidchatexercise;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.jakewharton.rxbinding.view.RxView;

import java.net.URISyntaxException;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.CompositeSubscription;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private CompositeSubscription bindingSubscriptions;
    private MessagesViewModel messagesViewModel;
    private Socket socket;

    private ArrayAdapter<String> arrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final View sendButton = findViewById(R.id.send_button);
        final EditText editText = (EditText) findViewById(R.id.edit_text);
        final ListView listView = (ListView) findViewById(R.id.list_view);

        arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        listView.setAdapter(arrayAdapter);

        try {
            socket = IO.socket("https://lit-everglades-74863.herokuapp.com");
        } catch (URISyntaxException e) {
            Log.e(TAG, "Error creating socket", e);
            finish();
        }

        RxView.clicks(sendButton)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(ev -> {
                    final String text = editText.getText().toString();
                    if (text.length() > 0) {
                        socket.emit("chat message", text);
                        editText.setText("");
                    }
                });

        Observable<String> messagesObservable = SocketUtil.createMessageListener(socket);
        messagesViewModel = new MessagesViewModel(messagesObservable);
        messagesViewModel.subscribe();
        socket.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindingSubscriptions = new CompositeSubscription();
        bindingSubscriptions.add(
                messagesViewModel
                        .getMessageList()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                messageList -> {
                                    arrayAdapter.clear();
                                    arrayAdapter.addAll(messageList);
                                })
        );
    }

    @Override
    protected void onPause() {
        super.onPause();
        bindingSubscriptions.unsubscribe();
        bindingSubscriptions = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (socket != null) {
            socket.disconnect();
            socket = null;
        }
        if (messagesViewModel != null) {
            messagesViewModel.unsubscribe();
            messagesViewModel = null;
        }
    }
}
