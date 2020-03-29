package com.andriod.wuziqi;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MessageAdapter extends BaseAdapter {
    private List<Message> messages;
   // private Message message;
    private Context context;

    public MessageAdapter(Context context) {
        this.context = context;
        messages = new ArrayList<>();
    }

    public void addMessage(Message message) {
        this.messages.add(message);
        notifyDataSetChanged();
    }

    public void clearScreen() {
        messages.clear();
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return messages.size();
    }

    @Override
    public Object getItem(int i) {
        return messages.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        LayoutInflater messageInflater = (LayoutInflater)context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        Message message = messages.get(i);

        if (message.getOwner().equals("myself")) {
            view = messageInflater.inflate(R.layout.my_message_layout, null);
            TextView message_body = view.findViewById(R.id.message_body);
            message_body.setText(message.getText());
        } else {
            view = messageInflater.inflate(R.layout.opponent_message_layout, null);
            TextView message_body = view.findViewById(R.id.opponent_message_body);
            message_body.setText(message.getText());
            TextView name = view.findViewById(R.id.name);
            name.setText(message.getOwner());
        }

        return view;
    }
}
