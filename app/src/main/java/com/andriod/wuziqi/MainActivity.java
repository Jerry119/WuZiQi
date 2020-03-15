package com.andriod.wuziqi;

import android.annotation.SuppressLint;
import android.content.Context;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;

import android.widget.ImageView;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;


public class MainActivity extends AppCompatActivity {

    private FirebaseDatabase database;
    private DatabaseReference myFirebaseRef;

   // private int numOfPlayers;
    private GridView gridView;
    private int playerID = 0;
    private String playerName;

    private String opponentName = null;

    private int[][] board_value;

    private final int[][] DIAGNAL_L = {{-1, -1}, {1, 1}};
    private final int[][] DIAGNAL_R = {{-1, 1}, {1, -1}};
    private final int[][] VERTICAL = {{-1, 0}, {1, 0}};
    private final int[][] HORIZONTAL = {{0, -1}, {0, 1}};

    private final int BOARD_SIZE = 13;
    //private final int BOARD_ROW_SIZE = 15;
    private final int FIRST_PLAYER = 1;
    private final int SECOND_PLAYER = 2;

    private boolean inGame = false;

    private int opponentPrevMove = -1;
    private int myPrevMove = -1;

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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //board_value = new int[BOARD_SIZE][BOARD_SIZE];

        database = FirebaseDatabase.getInstance();
        myFirebaseRef = database.getReference();

        gridView = findViewById(R.id.gridview);
        gridView.setAdapter(new TextAdapter(this));



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
                if (!inGame || myPrevMove == -1) {
                    Log.d("msg", "retract button is clicked, but not allowed yet");
                    return;
                }

            }
        });

        // get current number of player
        /*myFirebaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                numOfPlayers = dataSnapshot.child("num_of_players").getValue(Integer.class);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });*/


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

                if (!inGame)
                    return;

                boolean myTurn = dataSnapshot.child("Players").child(playerName).child("myTurn").getValue(boolean.class);

                Log.i("msg", playerName+"'s myTurn is "+myTurn);

                if (myTurn && inGame) {

                    // checking if opponent has moved
                    if (opponentName != null && dataSnapshot.child("Players").child(opponentName).hasChild("position")) {
                        // getting opponent's move
                        //int pos = dataSnapshot.child("Players").child(opponentName).child("position").getValue(Integer.class);
                        opponentPrevMove = dataSnapshot.child("Players").child(opponentName).child("position").getValue(Integer.class);

                        if (opponentPrevMove != -1) {

                            //myPrevMove = dataSnapshot.child("Players").child(playerName).child("position").getValue(Integer.class);
                            int row = opponentPrevMove / BOARD_SIZE;
                            int col = opponentPrevMove % BOARD_SIZE;

                            GridView gv = findViewById(R.id.gridview);
                            ImageView iv = gv.getChildAt(opponentPrevMove).findViewById(R.id.icon);

                            int opponentID;

                            // marking opponent's move in board_value and board
                            if (playerID == FIRST_PLAYER) {
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

                            Log.i("msg", "inside myTurn listener, myPrevMove is "+myPrevMove);

                            if (myPrevMove != -1) {
                                iv = gv.getChildAt(myPrevMove).findViewById(R.id.icon);
                                iv.setBackgroundResource(R.layout.grid_item_border);
                            }

                            // check if opponent won
                            if (isWinner(opponentID, opponentPrevMove) && inGame) {

                                inGame = false;

                                disableChessboard();

                                myFirebaseRef.child("Players").child(playerName).child("status").setValue("idle");

                                myFirebaseRef.child("Players").child(playerName).child("outcome").setValue("lose");


                                myFirebaseRef.child("Players").child(playerName).child("myTurn").setValue(false);

                                Toast.makeText(getApplicationContext(), "You lost!", Toast.LENGTH_SHORT).show();

                                return;
                            }
                        }
                    }

                    TextView result = findViewById(R.id.resultArea);
                    result.setText("It's your turn to move...");
                    enableChessBoard();
                } else {
                    if (inGame) {
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

                EditText user = findViewById(R.id.username);
                String pName = user.getText().toString();

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
                if (inGame)
                    return;

                String status;
                if (playerName == null || !dataSnapshot.hasChild("Players") || !dataSnapshot.child("Players").hasChild(playerName) || !dataSnapshot.child("Players").child(playerName).hasChild("status"))
                    return;

                String outcome = null;
                if (dataSnapshot.child("Players").child(playerName).hasChild("outcome"))
                    outcome = dataSnapshot.child("Players").child(playerName).child("outcome").getValue(String.class);

                /*
                 *
                 *      idle status
                 */
                status = dataSnapshot.child("Players").child(playerName).child("status").getValue(String.class);
                Log.i("msg", playerName+" is "+status);

                if (status != null && status.equals("idle")) {

                    //inGame = false;
                    if (outcome != null && outcome.equals("win")) {
                        TextView result = findViewById(R.id.resultArea);
                        result.setText(playerName+" has won!");
                    }
                    else if (outcome != null && outcome.equals("lose")) {
                        TextView result = findViewById(R.id.resultArea);
                        result.setText(opponentName+" has won!");
                    }

                    Log.i("msg", playerName+" is in idle status");
                    Log.i("msg", "opponent of "+playerName+": "+opponentName);

                    findViewById(R.id.ready).setClickable(true);
                    return;
                } else {
                    findViewById(R.id.ready).setClickable(false);
                }

                /*
                 *
                 *      ready status
                 */
                if (status.equals("ready")) {
                    updateOpponentName(myFirebaseRef);
                }

                /*
                 *
                 *      in game status
                 */
                if (status != null && status.equals("in game")) {


                    if (playerID == FIRST_PLAYER) {
                        if (opponentName.equals("no match yet")) {
                            updateOpponentName(myFirebaseRef);
                        }
                        //inGame = true;
                    }
                    else {
                        inGame = true;
                        if (dataSnapshot.child("Players").child(opponentName).child("outcome").getValue(String.class).equals("not set")) {
                            myFirebaseRef.child("Players").child(opponentName).child("status").setValue("in game");
                            myFirebaseRef.child("Players").child(opponentName).child("myTurn").setValue(true);
                            Log.i("msg", "inside status listener, setting player " + opponentName + "'s myTurn to true...");
                        }
                    }
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


    }

    public void updateOpponentName(DatabaseReference ref) {
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String status = dataSnapshot.child("Players").child(playerName).child("status").getValue(String.class);

                opponentName = dataSnapshot.child("Players").child(playerName).child("opponent").getValue(String.class);

                if (opponentName.equals("no match yet") && status.equals("ready")) {
                    for (DataSnapshot ds : dataSnapshot.child("Players").getChildren()) {
                        // can't have self as opponent
                        if (ds.getKey().equals(playerName)) continue;

                        // check if opponent's status is ready
                        if (!ds.hasChild("status") || !(ds.child("status").getValue(String.class)).equals("ready"))
                            continue;

                        // found the opponent
                        opponentName = ds.getKey();
                        // save opponent name to database
                        myFirebaseRef.child("Players").child(playerName).child("opponent").setValue(opponentName);

                        myFirebaseRef.child("Players").child(opponentName).child("opponent").setValue(playerName);
                        break;
                    }
                }

                Log.i("msg", "inside updateOpponentName(), "+playerName+"'s ID is "+playerID);

                if (!inGame && playerID == FIRST_PLAYER && opponentName != null && !opponentName.equals("no match yet")) {
                    inGame = true;
                    TextView p2 = findViewById(R.id.p2);
                    p2.setText("Player2: " + opponentName);
                    Toast.makeText(getApplicationContext(), "Game starts...", Toast.LENGTH_SHORT).show();
                }

                if (playerID != 0)
                    return;

                if (opponentName.equals("no match yet")) {
                    playerID = FIRST_PLAYER;
                    TextView p1 = findViewById(R.id.p1);
                    p1.setText("Player1: " + playerName);
                    TextView result = findViewById(R.id.resultArea);
                    result.setText("You are player" + playerID + ". Waiting for one more player...");
                }
                else {
                    playerID = SECOND_PLAYER;
                    TextView p2 = findViewById(R.id.p2);
                    p2.setText("Player2: " + playerName);
                    TextView p1 = findViewById(R.id.p1);
                    p1.setText("Player1: " + opponentName);

                    myFirebaseRef.child("Players").child(playerName).child("status").setValue("in game");
                    //inGame = true;

                    TextView result = findViewById(R.id.resultArea);
                    result.setText("You are player" + playerID + ". Waiting for your turn to move...");
                    Toast.makeText(getApplicationContext(), "Game starts...", Toast.LENGTH_SHORT).show();
                }

                Log.i("msg", playerName+" is ready...");
                Log.i("msg", "opponentName of "+playerName+": "+opponentName);
                Log.i("msg", playerName+"'s ID is "+playerID);
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

                if (dataSnapshot.child("Players").hasChild(pName) || pName.equals("no match yet")) {
                    Toast.makeText(getApplicationContext(), "The name is registered. Please try another name.", Toast.LENGTH_SHORT).show();
                } else {
                    playerName = pName;
                }

                if (playerName == null) {
                    return;
                }


                // numOfPlayers++;
                //myFirebaseRef.child("num_of_players").setValue(numOfPlayers);

                myFirebaseRef.child("Players").child(playerName).child("status").setValue("idle");
                TextView welcome = findViewById(R.id.welcome_msg);
                welcome.setText("Welcome to the game, "+playerName);

                TextView tv = findViewById(R.id.resultArea);
                tv.setText("Click ready to start the game.");

                Button registerButton = findViewById(R.id.register);
                registerButton.setClickable(false);

                EditText user = findViewById(R.id.username);
                user.setVisibility(View.GONE);
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

                board_value[row][col] = playerID;

                ImageView icon = view.findViewById(R.id.icon);
                if (playerID == FIRST_PLAYER) {
                    icon.setImageResource(R.drawable.x_icon);
                    icon.setBackgroundColor(R.drawable.player_move_bg);
                }
                else {
                    icon.setImageResource(R.drawable.check);
                    icon.setBackgroundColor(R.drawable.player_move_bg);
                }

                myPrevMove = i;

                Log.d("msg", "inside enableChessBoard() of "+playerName+", opponentPrevMove is "+opponentPrevMove);

                if (opponentPrevMove != -1) {
                    GridView gv = findViewById(R.id.gridview);
                    ImageView iv = gv.getChildAt(opponentPrevMove).findViewById(R.id.icon);
                    //iv.setBackgroundColor(0);
                    iv.setBackgroundResource(R.layout.grid_item_border);
                }

                // current player has won the game
                if (isWinner(playerID, i)) {
                    disableChessboard();
                    Toast.makeText(getApplicationContext(), "You won!", Toast.LENGTH_SHORT).show();
                    myFirebaseRef.child("Players").child(playerName).child("status").setValue("idle");

                    myFirebaseRef.child("Players").child(playerName).child("outcome").setValue("win");
                    inGame = false;
                }


                // alternating the turn
                myFirebaseRef.child("Players").child(playerName).child("myTurn").setValue(false);
                Log.i("msg", "inside enableChessBoard(), setting "+playerName+"'s myTurn to false"  );

                // storing current player's move
                myFirebaseRef.child("Players").child(playerName).child("position").setValue(i);

                myFirebaseRef.child("Players").child(opponentName).child("myTurn").setValue(true);
                Log.i("msg", "inside enableChessBoard(), setting "+opponentName+"'s myTurn to true"  );

            }
        });
    }

    public void onDestroy() {
        FirebaseDatabase.getInstance().getReference().child("Players").child(playerName).removeValue();
        Log.i("msg", "in onDestroy()");
        super.onDestroy();
    }

    @SuppressLint("ResourceType")
    public void resetChessBoard() {
        opponentName = null;
        opponentPrevMove = -1;
        myPrevMove = -1;
        playerID = 0;

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
        myFirebaseRef.child("Players").child(playerName).child("outcome").setValue("not set");
        myFirebaseRef.child("Players").child(playerName).child("position").setValue(-1);
        myFirebaseRef.child("Players").child(playerName).child("myTurn").setValue(false);
    }

}