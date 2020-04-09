package com.andriod.wuziqi;

import android.annotation.SuppressLint;
import android.app.Activity;

import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.Fragment;
import android.content.Context;

import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;

import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;


public class MainActivity extends AppCompatActivity implements Animation.AnimationListener {

    private FirebaseDatabase database;
    private DatabaseReference myFirebaseRef;

    private GridView gridView;
    private ListView listView;

    private String playerName;

    private int[][] board_value;

    private final int[][] DIAGNAL_L = {{-1, -1}, {1, 1}};
    private final int[][] DIAGNAL_R = {{-1, 1}, {1, -1}};
    private final int[][] VERTICAL = {{-1, 0}, {1, 0}};
    private final int[][] HORIZONTAL = {{0, -1}, {0, 1}};

    protected final int BOARD_SIZE = 11;

    //private final int BOARD_ROW_SIZE = 15;
    private final int FIRST_PLAYER = 1;
    private final int SECOND_PLAYER = 2;

    private User user;

    private Animation animation;

    private MessageAdapter messageAdapter;

    private LinearLayout gameWindow;
    private LinearLayout chatWindow;


    @Override
    public void onAnimationStart(Animation animation) {

    }

    @Override
    public void onAnimationEnd(Animation animation) {

    }

    @Override
    public void onAnimationRepeat(Animation animation) {

    }

    protected enum Status {
        idle, in_game, ready, disconnected;
    }


    private class TextAdapter extends BaseAdapter {
        Context context;
        LayoutInflater inflater;

        public TextAdapter(Context context) {
            this.context = context;
            this.inflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return BOARD_SIZE * BOARD_SIZE;
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }


        @SuppressLint("ResourceType")
        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            view = inflater.inflate(R.layout.board_button, null);
            ImageView icon = (ImageView) view.findViewById(R.id.icon);
            icon.setBackgroundResource(R.layout.grid_item_border);

            return view;
        }
    }

    // check if current player is a winner
    private boolean isWinner(int value, int pos) {
        return checkIfWin(DIAGNAL_L, board_value, value, pos) ||
                checkIfWin(DIAGNAL_R, board_value, value, pos) ||
                checkIfWin(VERTICAL, board_value, value, pos) ||
                checkIfWin(HORIZONTAL, board_value, value, pos);
    }

    // helper for isWinner()
    @SuppressLint("ResourceType")
    private boolean checkIfWin(int[][] dir, int[][] board, int value, int pos) {
        int row = pos / BOARD_SIZE;
        int col = pos % BOARD_SIZE;

        List<int[]> list = new ArrayList<>();

        Queue<int[]> queue = new LinkedList<>();
        int count = 1;
        for (int i = 0; i < 2; i++) {
            queue.add(new int[]{row, col});
            while (!queue.isEmpty()) {
                int[] node = queue.poll();
                list.add(node);

                int n_r = node[0] + dir[i][0];
                int n_c = node[1] + dir[i][1];

                if (!isInBound(n_r, n_c)) continue;
                if (value != board[n_r][n_c]) continue;

                queue.add(new int[]{n_r, n_c});
                count++;
            }
        }
        if (count >= 5) {
            GridView gv = findViewById(R.id.gridview);
            for (int[] node : list) {
                int p = node[0] * BOARD_SIZE + node[1];
                gv.getChildAt(p).findViewById(R.id.icon).setBackgroundResource(R.drawable.winning_button_color);
            }

            return true;
        }

        return false;
    }

    private boolean isInBound(int r, int c) {
        if (r < 0 || r >= BOARD_SIZE || c < 0 || c >= BOARD_SIZE)
            return false;
        return true;
    }


    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        database = FirebaseDatabase.getInstance();
        myFirebaseRef = database.getReference();

        gridView = findViewById(R.id.gridview);
        gridView.setAdapter(new TextAdapter(this));

        user = new User();

        gameWindow = findViewById(R.id.game_window);
        chatWindow = findViewById(R.id.chat_window);

        /*
         ******** Ready Button *********
         */
        Button readyButton = findViewById(R.id.ready);
        readyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (playerName == null)
                    return;

                resetChessBoard();

            }
        });

        /*
         *
         * *********** Retract button ************
         */
        Button retractButton = findViewById(R.id.retract);
        retractButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!user.getPlaying() || user.getMyPrevMove() == -1) {
                    Log.d("msg", "retract button is clicked, but not allowed yet");
                    Toast.makeText(getApplicationContext(), "not allowed yet", Toast.LENGTH_SHORT).show();
                    return;
                }
                FirebaseDatabase.getInstance().getReference().addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        boolean canRequestRetract = dataSnapshot.child("Players").child(playerName).child("canRequestRetract").getValue(boolean.class);

                        boolean myTurn = dataSnapshot.child("Players").child(playerName).child("myTurn").getValue(Boolean.class);
                        if (!myTurn && canRequestRetract) {
                            Toast.makeText(getApplicationContext(), "You have requested to retract a move", Toast.LENGTH_SHORT).show();

                            myFirebaseRef.child("Players").child(user.getOpponentName()).child("receiveRetractRequest").setValue(true);

                            myFirebaseRef.child("Players").child(playerName).child("canRequestRetract").setValue(false);
                            myFirebaseRef.removeEventListener(this);

                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

            }
        });

        /*
         *
         * ********** receiveRetractRequest listener *****************
         */
        myFirebaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (playerName == null || !dataSnapshot.child("Players").child(playerName).hasChild("receiveRetractRequest"))
                    return;

                boolean receiveRetractRequest = dataSnapshot.child("Players").child(playerName).child("receiveRetractRequest").getValue(boolean.class);

                // retract request is received from opponent
                if (receiveRetractRequest) {

                    Log.i("msg", playerName + " receives a retract request");

                    myFirebaseRef.child("Players").child(playerName).child("receiveRetractRequest").setValue(false);

                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
                    alertDialogBuilder.setMessage(user.getOpponentName() + " wants to retract a move.");

                    // player allows opponent to retract a move
                    alertDialogBuilder.setPositiveButton("allow", new DialogInterface.OnClickListener() {
                        @SuppressLint("ResourceType")
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            myFirebaseRef.child("Players").child(playerName).child("myTurn").setValue(false);
                            myFirebaseRef.child("Players").child(user.getOpponentName()).child("myTurn").setValue(true);
                            myFirebaseRef.child("Players").child(user.getOpponentName()).child("allowedRetract").setValue("true");

                            // clear opponent's previous move
                            GridView gv = findViewById(R.id.gridview);
                            ImageView iv = gv.getChildAt(user.getOpponentPrevMove()).findViewById(R.id.icon);
                            iv.setImageResource(0);
                            iv.setBackgroundResource(R.layout.grid_item_border);
                            int r = user.getOpponentPrevMove() / BOARD_SIZE;
                            int c = user.getOpponentPrevMove() % BOARD_SIZE;
                            board_value[r][c] = 0;

                            if (user.getMyPrevMove() != -1) {
                                ImageView iv1 = gv.getChildAt(user.getMyPrevMove()).findViewById(R.id.icon);
                                iv1.setBackgroundColor(R.drawable.player_move_bg);
                            }
                        }
                    });

                    // player doesn't allow opponent to retract
                    alertDialogBuilder.setNegativeButton("decline", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                            myFirebaseRef.child("Players").child(user.getOpponentName()).child("allowedRetract").setValue("false");
                        }
                    });
                    AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.show();
                    alertDialog.setCancelable(false);
                    alertDialog.setCanceledOnTouchOutside(false);

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        /*
         *
         * ********** allowedRetract listener ***********
         */
        myFirebaseRef.addValueEventListener(new ValueEventListener() {
            @SuppressLint("ResourceType")
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (playerName == null || !dataSnapshot.child("Players").child(playerName).hasChild("allowedRetract") || !user.getPlaying())
                    return;

                String allowedRetract = dataSnapshot.child("Players").child(playerName).child("allowedRetract").getValue(String.class);

                if (allowedRetract.equals("not set"))
                    return;

                // clear my previous move
                if (allowedRetract.equals("true")) {
                    GridView gv = findViewById(R.id.gridview);
                    ImageView iv = gv.getChildAt(user.getMyPrevMove()).findViewById(R.id.icon);
                    iv.setImageResource(0);
                    iv.setBackgroundResource(R.layout.grid_item_border);
                    int r = user.getMyPrevMove() / BOARD_SIZE;
                    int c = user.getMyPrevMove() % BOARD_SIZE;
                    board_value[r][c] = 0;

                } else {
                    Toast.makeText(getApplicationContext(), user.getOpponentName() + " didn't allow you to retract", Toast.LENGTH_SHORT).show();
                }
                myFirebaseRef.child("Players").child(playerName).child("allowedRetract").setValue("not set");
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


        /*
         ********** myTurn listener ************
         */
        myFirebaseRef.addValueEventListener(new ValueEventListener() {
            @SuppressLint("ResourceType")
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if (playerName == null || !dataSnapshot.child("Players").child(playerName).hasChild("myTurn")) {
                    return;
                }

                if (!user.getPlaying())
                    return;

                boolean myTurn = dataSnapshot.child("Players").child(playerName).child("myTurn").getValue(boolean.class);

                Log.i("msg", playerName + "'s myTurn is " + myTurn);

                if (myTurn) {

                    // checking if opponent has moved
                    if (user.getOpponentName() != null){
                        int opponentPrevMove = dataSnapshot.child("Players").child(user.getOpponentName()).child("position").getValue(Integer.class);

                        user.setOpponentPrevMove(opponentPrevMove);

                        if (opponentPrevMove != -1) {
                            int row = opponentPrevMove / BOARD_SIZE;
                            int col = opponentPrevMove % BOARD_SIZE;

                            GridView gv = findViewById(R.id.gridview);
                            ImageView iv = gv.getChildAt(opponentPrevMove).findViewById(R.id.icon);

                            int opponentID;

                            // marking opponent's move in board_value and board
                            if (user.getUserID() == FIRST_PLAYER) {
                                opponentID = SECOND_PLAYER;
                                board_value[row][col] = SECOND_PLAYER;
                                iv.setImageResource(R.drawable.check);
                                iv.setBackgroundColor(R.drawable.opponent_move_bg);
                            } else {
                                opponentID = FIRST_PLAYER;
                                board_value[row][col] = FIRST_PLAYER;
                                iv.setImageResource(R.drawable.x_icon);
                                iv.setBackgroundColor(R.drawable.opponent_move_bg);
                            }


                            if (user.getMyPrevMove() != -1) {
                                iv = gv.getChildAt(user.getMyPrevMove()).findViewById(R.id.icon);
                                iv.setBackgroundResource(R.layout.grid_item_border);
                            }


                            // start blinking effect for my turn to move
                            if (gameWindow.getVisibility() == View.GONE) {
                                TextView game_frag = findViewById(R.id.game_fragment);
                                startAnimationEffect(game_frag);
                            }

                            // check if opponent won
                            if (isWinner(opponentID, opponentPrevMove)) {
                                user.setPlaying(false);

                                disableChessboard();

                                myFirebaseRef.child("Players").child(playerName).child("status").setValue("idle");
                                user.setStatus(Status.idle);

                                myFirebaseRef.child("Players").child(playerName).child("outcome").setValue("lose");


                                myFirebaseRef.child("Players").child(playerName).child("myTurn").setValue(false);

                                Toast.makeText(getApplicationContext(), "You lost!", Toast.LENGTH_SHORT).show();

                                return;
                            }
                            // chessboard is filled, it's a tie game
                            if (checkIfTied()) {
                                Log.i("msg", "it's a tie");

                                TextView tv = findViewById(R.id.resultArea);
                                tv.setText("It's a tied game.");
                                Toast.makeText(getApplicationContext(), "no winner is determined", Toast.LENGTH_SHORT).show();
                                user.setPlaying(false);

                                myFirebaseRef.child("Players").child(playerName).child("status").setValue("idle");
                                user.setStatus(Status.idle);

                                myFirebaseRef.child("Players").child(playerName).child("outcome").setValue("no set");

                                myFirebaseRef.child("Players").child(playerName).child("canRequestRetract").setValue(false);

                                myFirebaseRef.child("Players").child(user.getOpponentName()).child("canRequestRetract").setValue(false);

                                myFirebaseRef.child("Players").child(playerName).child("myTurn").setValue(false);

                                return;
                            }

                        }
                    }

                    TextView result = findViewById(R.id.resultArea);
                    result.setText("It's your turn to move...");

                    enableChessBoard();
                } else {
                    if (user.getPlaying()) {
                        TextView result = findViewById(R.id.resultArea);
                        result.setText("Wait for your turn to move...");
                    }
                    disableChessboard();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


        /*
         ********* Register Button *********
         */
        final Button registerButton = findViewById(R.id.register);
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                board_value = new int[BOARD_SIZE][BOARD_SIZE];

                EditText username = findViewById(R.id.username);
                String pName = username.getText().toString();

                // have to have a name for player
                if (pName.length() == 0)
                    return;


                // check if the name has been registered
                checkIfPlayerNameExist(pName);

            }

        });

        /*
         *
         *      game status listener
         *
         */

        myFirebaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (user.getPlaying())
                    return;

                if (playerName == null || !dataSnapshot.hasChild("Players") || !dataSnapshot.child("Players").hasChild(playerName) || !dataSnapshot.child("Players").child(playerName).hasChild("status"))
                    return;

                String outcome = null;
                if (dataSnapshot.child("Players").child(playerName).hasChild("outcome"))
                    outcome = dataSnapshot.child("Players").child(playerName).child("outcome").getValue(String.class);

                Status s = user.getStatus();

                switch (s) {
                    case idle:

                        if (outcome != null && outcome.equals("win")) {
                            TextView result = findViewById(R.id.resultArea);
                            result.setText(playerName + " has won!");
                        } else if (outcome != null && outcome.equals("lose")) {
                            TextView result = findViewById(R.id.resultArea);
                            result.setText(user.getOpponentName() + " has won!");
                        }

                        Log.i("msg", playerName + " is in idle status");

                        findViewById(R.id.ready).setClickable(true);
                        break;

                    case ready:

                        findViewById(R.id.ready).setClickable(false);
                        updateOpponentName();
                        break;

                    case in_game:
                        findViewById(R.id.ready).setClickable(false);


                        if (user.getUserID() == FIRST_PLAYER) {
                            if (user.getOpponentName().equals("no match yet")) {

                                updateOpponentName();
                            }
                        } else if (user.getUserID() == SECOND_PLAYER) {
                            user.setPlaying(true);
                            Log.i("debug", "here");
                            Log.i("debug", "opponentName is "+user.getOpponentName());

                            if (!dataSnapshot.child("Players").hasChild(user.getOpponentName()))
                                return;
                            if (dataSnapshot.child("Players").child(user.getOpponentName()).child("outcome").getValue(String.class).equals("not set")) {
                                myFirebaseRef.child("Players").child(user.getOpponentName()).child("status").setValue("in_game");

                                myFirebaseRef.child("Players").child(user.getOpponentName()).child("myTurn").setValue(true);
                                Log.i("msg", "inside status listener, setting player " + user.getOpponentName() + "'s myTurn to true...");
                            }
                        }
                        break;
                    default:
                }


            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


        /*
         *
         * *********** disconnected listener
         */
        myFirebaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!user.getPlaying() || user.getStatus() != Status.in_game)
                    return;
                String opponentStatus = dataSnapshot.child("Players").child(user.getOpponentName()).child("status").getValue(String.class);
                if (Status.valueOf(opponentStatus) == Status.disconnected) {
                    Toast.makeText(getApplicationContext(), user.getOpponentName() + " has just quit the game. You won!", Toast.LENGTH_SHORT).show();
                    user.setPlaying(false);

                    myFirebaseRef.child("Players").child(playerName).child("myTurn").setValue(false);
                    myFirebaseRef.child("Players").child(playerName).child("status").setValue("idle");
                    user.setStatus(Status.idle);

                    myFirebaseRef.child("Players").child(playerName).child("outcome").setValue("win");

                    disableChessboard();
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        messageAdapter = new MessageAdapter(this);
        listView = findViewById(R.id.message_view);
        listView.setAdapter(messageAdapter);
        listView.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);
        listView.setStackFromBottom(true);


        /*
         *
         * *********** my message listener
         */
        myFirebaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.hasChild("Players") || playerName == null)
                    return;
                Status s = user.getStatus();

                if (s == Status.in_game || (s == Status.idle && !user.getOpponentName().equals("no match yet"))) {
                    String myMsg = dataSnapshot.child("Players").child(playerName).child("message").getValue(String.class);
                    if (myMsg != null && !myMsg.equals("RESERVED_MSG")) {
                        Message message = new Message(myMsg, "myself");
                        messageAdapter.addMessage(message);
                        myFirebaseRef.child("Players").child(playerName).child("message").setValue("RESERVED_MSG");
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        /*
         *
         * ************* opponent message listener
         */
        myFirebaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.hasChild("Players") || playerName == null || user.getOpponentName().equals("no match yet"))
                    return;

                Status s = user.getStatus();

                String opponentMsg = dataSnapshot.child("Players").child(user.getOpponentName()).child("message").getValue(String.class);
                if (opponentMsg != null && !opponentMsg.equals("RESERVED_MSG")) {
                    Message message = new Message(opponentMsg, user.getOpponentName());
                    messageAdapter.addMessage(message);
                    if (chatWindow.getVisibility() == View.GONE) {
                        TextView chat_frag = findViewById(R.id.chat_fragment);
                        startAnimationEffect(chat_frag);
                    }
                }


            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        ImageButton sendButton = findViewById(R.id.send);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Status s = user.getStatus();
                if (s == Status.ready || (s == Status.idle && user.getOpponentName().equals("no match yet"))) {
                    Toast.makeText(getApplicationContext(), "Wait until you are in a game", Toast.LENGTH_SHORT).show();
                    return;
                }

                EditText msg = findViewById(R.id.message);
                String message = msg.getText().toString();
                if (message.length() == 0)
                    return;
                myFirebaseRef.child("Players").child(playerName).child("message").setValue(message);
                msg.setText("");
                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                View focusedView = getCurrentFocus();
                if (focusedView != null)
                    inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
        });

        final TextView chat_window = findViewById(R.id.chat_fragment);
        chat_window.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("ResourceType")
            @Override
            public void onClick(View view) {
                gameWindow.setVisibility(View.GONE);
                chatWindow.setVisibility(View.VISIBLE);

                if (animation != null && animation.hasStarted()) {
                    chat_window.clearAnimation();
                }

                chat_window.setBackgroundResource(R.drawable.current_fragment_color);
                TextView game = findViewById(R.id.game_fragment);
                game.setBackgroundResource(R.drawable.unused_fragment_color);

            }
        });


        final TextView game_window = findViewById(R.id.game_fragment);
        game_window.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                chatWindow.setVisibility(View.GONE);
                gameWindow.setVisibility(View.VISIBLE);

                game_window.setBackgroundResource(R.drawable.current_fragment_color);

                if (animation != null && animation.hasStarted()) {
                    game_window.clearAnimation();
                }

                TextView chat = findViewById(R.id.chat_fragment);
                chat.setBackgroundResource(R.drawable.unused_fragment_color);
            }
        });
    }

    public void startAnimationEffect(TextView textView) {
        animation = AnimationUtils.loadAnimation(this, R.anim.blink_effect);
        animation.setAnimationListener(this);
        textView.startAnimation(animation);
    }

    public void updateOpponentName() {
        myFirebaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (user.getPlaying())
                    return;

                Status status = user.getStatus();

                String opponentName;

                opponentName = dataSnapshot.child("Players").child(playerName).child("opponent").getValue(String.class);

                if (opponentName.equals("no match yet") && status == Status.ready) {
                    for (DataSnapshot ds : dataSnapshot.child("Players").getChildren()) {
                        // can't have self as opponent
                        if (ds.getKey().equals(playerName)) continue;

                        // check if opponent's status is ready
                        if (!ds.hasChild("status") || !(ds.child("status").getValue(String.class)).equals("ready"))
                            continue;

                        // found the opponent
                        opponentName = ds.getKey();
                        user.setOpponentName(opponentName);

                        // save opponent name to database
                        myFirebaseRef.child("Players").child(playerName).child("opponent").setValue(opponentName);

                        myFirebaseRef.child("Players").child(opponentName).child("opponent").setValue(playerName);
                        break;
                    }
                }


                if (user.getUserID() == FIRST_PLAYER ) {
                    if (!opponentName.equals("no match yet")) {
                        TextView p2 = findViewById(R.id.p2);
                        p2.setText("Player2: " + opponentName);
                        Toast.makeText(getApplicationContext(), "Game starts...", Toast.LENGTH_SHORT).show();
                        user.setPlaying(true);
                        user.setStatus(Status.in_game);
                    }
                    else {
                        TextView p2 = findViewById(R.id.p2);
                        p2.setText("Player2: ");
                    }
                }


                if (user.getUserID() != 0)
                    return;

                if (opponentName.equals("no match yet")) {
                    user.setUserID(FIRST_PLAYER);
                    TextView p1 = findViewById(R.id.p1);
                    p1.setText("Player1: " + playerName);
                    TextView result = findViewById(R.id.resultArea);
                    result.setText("You are player" + user.getUserID() + ". Waiting for one more player...");
                }
                else {
                    Log.i("debug", "inside updateOpponent, opponentName is "+opponentName);

                    //playerID = SECOND_PLAYER;
                    user.setUserID(SECOND_PLAYER);
                    TextView p2 = findViewById(R.id.p2);
                    p2.setText("Player2: " + playerName);
                    TextView p1 = findViewById(R.id.p1);
                    p1.setText("Player1: " + opponentName);

                    myFirebaseRef.child("Players").child(playerName).child("status").setValue("in_game");
                    user.setStatus(Status.in_game);

                    TextView result = findViewById(R.id.resultArea);
                    result.setText("You are player" + user.getUserID() + ". Waiting for your turn to move...");
                    Toast.makeText(getApplicationContext(), "Game starts...", Toast.LENGTH_SHORT).show();
                }

                Log.i("msg", playerName+" is ready...");

                myFirebaseRef.removeEventListener(this);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }


    // check if the playerName already exists in data base
    public void checkIfPlayerNameExist(final String pName) {
        FirebaseDatabase.getInstance().getReference().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                Log.i("msg", "checking if pName " + pName + " exists in database");

                if (pName.equals("no match yet") ||
                        (dataSnapshot.child("Players").hasChild(pName) && !dataSnapshot.child("Players").child(pName).child("status").getValue(String.class).equals("disconnected"))) {
                    Toast.makeText(getApplicationContext(), "The name is registered. Please try another name.", Toast.LENGTH_SHORT).show();
                } else {
                    playerName = pName;
                    user.setUsername(pName);
                }

                if (user.getUsername() == null)
                    return;

                myFirebaseRef.child("Players").child(pName).child("status").setValue("idle");
                user.setStatus(Status.idle);

                TextView welcome = findViewById(R.id.welcome_msg);
                welcome.setText("Welcome to the game, "+pName);

                TextView tv = findViewById(R.id.resultArea);
                tv.setText("Click ready to start the game.");

                Button registerButton = findViewById(R.id.register);
                registerButton.setClickable(false);

                EditText et = findViewById(R.id.username);
                et.setVisibility(View.GONE);
                registerButton.setVisibility(View.GONE);

                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public void disableChessboard() {
        gridView.setOnItemClickListener(null);
    }

    // listener for the chess board
    public void enableChessBoard() {
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @SuppressLint("ResourceType")
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                int row = i / BOARD_SIZE;
                int col = i % BOARD_SIZE;

                // the cell is occupied, do nothing
                if (board_value[row][col] != 0) {
                    return;
                }

                board_value[row][col] = user.getUserID();

                ImageView icon = view.findViewById(R.id.icon);
                if (user.getUserID() == FIRST_PLAYER) {
                    icon.setImageResource(R.drawable.x_icon);
                    icon.setBackgroundColor(R.drawable.player_move_bg);
                }
                else {
                    icon.setImageResource(R.drawable.check);
                    icon.setBackgroundColor(R.drawable.player_move_bg);
                }

                user.setMyPrevMove(i);

                if (user.getOpponentPrevMove() != -1) {
                    GridView gv = findViewById(R.id.gridview);
                    ImageView iv = gv.getChildAt(user.getOpponentPrevMove()).findViewById(R.id.icon);
                    iv.setBackgroundResource(R.layout.grid_item_border);
                }

                // current player has won the game
                if (isWinner(user.getUserID(), i)) {
                    disableChessboard();
                    Toast.makeText(getApplicationContext(), "You won!", Toast.LENGTH_SHORT).show();
                    myFirebaseRef.child("Players").child(playerName).child("status").setValue("idle");
                    user.setStatus(Status.idle);

                    myFirebaseRef.child("Players").child(playerName).child("outcome").setValue("win");
                    //inGame = false;
                    user.setPlaying(false);
                }

                // chessboard is filled, it's a tie game
                if (user.getPlaying() && checkIfTied()) {
                    TextView result = findViewById(R.id.resultArea);
                    result.setText("It's a tied game.");
                    Toast.makeText(getApplicationContext(), "no winner is determined", Toast.LENGTH_SHORT).show();

                    user.setPlaying(false);

                    myFirebaseRef.child("Players").child(playerName).child("status").setValue("idle");
                    user.setStatus(Status.idle);

                    myFirebaseRef.child("Players").child(playerName).child("position").setValue(i);

                    myFirebaseRef.child("Players").child(playerName).child("outcome").setValue("no set");

                    myFirebaseRef.child("Players").child(playerName).child("canRequestRetract").setValue(false);

                    myFirebaseRef.child("Players").child(user.getOpponentName()).child("canRequestRetract").setValue(false);

                    myFirebaseRef.child("Players").child(playerName).child("myTurn").setValue(false);

                    myFirebaseRef.child("Players").child(user.getOpponentName()).child("myTurn").setValue(true);

                    return;
                }


                // alternating the turn
                myFirebaseRef.child("Players").child(playerName).child("myTurn").setValue(false);
                Log.i("msg", "inside enableChessBoard(), setting "+playerName+"'s myTurn to false"  );

                // allow player to request retract after his move
                myFirebaseRef.child("Players").child(playerName).child("canRequestRetract").setValue(true);

                // storing current player's move
                myFirebaseRef.child("Players").child(playerName).child("position").setValue(i);

                myFirebaseRef.child("Players").child(user.getOpponentName()).child("myTurn").setValue(true);

                myFirebaseRef.child("Players").child(user.getOpponentName()).child("canRequestRetract").setValue(false);

                Log.i("msg", "inside enableChessBoard(), setting "+user.getOpponentName()+"'s myTurn to true"  );

            }
        });

    }

    public boolean checkIfTied() {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board_value[i][j] == 0)
                    return false;
            }
        }
        return true;
    }


    public void onDestroy() {
        if (playerName != null) {
            myFirebaseRef.child("Players").child(playerName).child("status").setValue("disconnected");
        }
        super.onDestroy();
    }

    @SuppressLint("ResourceType")
    public void resetChessBoard() {

        user.setUserID(0);

        if (!user.getOpponentName().equals("no match yet"))
            myFirebaseRef.child("Players").child(user.getOpponentName()).child("opponent").setValue("no match yet");

        user.setOpponentName("no match yet");
        user.setMyPrevMove(-1);
        user.setOpponentPrevMove(-1);

        messageAdapter.clearScreen();

        GridView gv = findViewById(R.id.gridview);


        Log.i("msg", "board size: "+board_value.length);

        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board_value[i][j] != 0) {
                    board_value[i][j] = 0;
                    int pos = i*BOARD_SIZE+j;
                    ImageView iv = gv.getChildAt(pos).findViewById(R.id.icon);
                    iv.setImageResource(0);
                    ((ImageView)iv).setBackgroundResource(R.layout.grid_item_border);
                }
            }
        }

        disableChessboard();
        myFirebaseRef.child("Players").child(playerName).child("opponent").setValue("no match yet");

        myFirebaseRef.child("Players").child(playerName).child("status").setValue("ready");
        user.setStatus(Status.ready);

        myFirebaseRef.child("Players").child(playerName).child("outcome").setValue("not set");
        myFirebaseRef.child("Players").child(playerName).child("position").setValue(-1);
        myFirebaseRef.child("Players").child(playerName).child("myTurn").setValue(false);
        myFirebaseRef.child("Players").child(playerName).child("receiveRetractRequest").setValue(false);
        myFirebaseRef.child("Players").child(playerName).child("canRequestRetract").setValue(false);
        myFirebaseRef.child("Players").child(playerName).child("allowedRetract").setValue("not set");
        myFirebaseRef.child("Players").child(playerName).child("message").setValue("RESERVED_MSG");

    }

}