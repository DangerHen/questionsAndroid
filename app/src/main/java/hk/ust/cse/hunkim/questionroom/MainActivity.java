package hk.ust.cse.hunkim.questionroom;


import android.app.ListActivity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;

import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;

import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Button;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import hk.ust.cse.hunkim.questionroom.db.DBHelper;
import hk.ust.cse.hunkim.questionroom.db.DBUtil;
import hk.ust.cse.hunkim.questionroom.question.Question;
import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends ListActivity {
    private String mUsername;
    private boolean incognitoMode = false;
    private String mRoomName;
    private QuestionAdapter mQuestionAdapter;
    private RESTfulAPI mAPI;
    private DBUtil dbutil;
    private Socket mSocket;
    private String image = null;
    public DBUtil getDbutil() {
        return dbutil;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        dbutil = new DBUtil(new DBHelper(this));
        Intent intent = getIntent();
        assert (intent != null);

        // Make it a bit more reliable
        mRoomName = intent.getStringExtra(JoinActivity.ROOM_NAME);
        if (mRoomName == null || mRoomName.length() == 0) {
            mRoomName = "all";
        }
        findViewById(R.id.reset_search).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Reset_Search();
            }
        });
        mUsername = intent.getExtras().getString("Username");
        mQuestionAdapter = new QuestionAdapter(this, new ArrayList<Question>());
        mAPI = RESTfulAPI.getInstance();
        createSocketEventListener();
        setTitle("Room name: " + mRoomName);

        ListView listView = getListView();
        listView.setAdapter(mQuestionAdapter);

        Map<String, String> query = new ArrayMap<>();
        query.put("roomName", mRoomName);
        mAPI.getQuestionList(query).enqueue(new Callback<List<Question>>() {
            @Override
            public void onResponse(Response<List<Question>> response, Retrofit retrofit) {
                List<Question> questions = response.body();
                if (questions != null) {
                    mQuestionAdapter.setQuestionList(questions);
                } else {
                    Log.e("Empty response body", "Null question list");
                }
            }

            @Override
            public void onFailure(Throwable t) {

            }
        });

        // Setup our input methods. Enter key on the keyboard or pushing the send button
    /*    EditText inputText = (EditText) findViewById(R.id.messageInput);
        inputText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_NULL && keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                    sendMessage();
                }
                return true;
            }
        });
*/
/*        findViewById(R.id.sendButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage();
            }
        });*/
        findViewById(R.id.DrawButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BeginDrawing();
            }
        });
    }

    /*
    @Override
    public void onStart() {
        super.onStart();

        // Setup our view and list adapter. Ensure it scrolls to the bottom as data changes
        final ListView listView = getListView();
        // Tell our list adapter that we only want 200 messages at a time
        Map<String, String> query = new ArrayMap<>();
        query.put("sortBy", "order");
        query.put("limit", "200");
        mQuestionAdapter = new QuestionAdapter(getBaseContext(), mAPI.getQuestionList(query));
        listView.setAdapter(mQuestionAdapter);
        mQuestionAdapter.notifyDataSetChanged(); // ??? needed ???
    }
    */

    public void Reset_Search() {
        Map<String, String> query = new LinkedHashMap<>(); // use LinkedHashMap because the insertion order of sortBy and order should be maintained
        query.put("roomName", mRoomName);
        query.put("sortBy", "echo");
        query.put("order", "-1"); // -1 for descending order
        query.put("sortBy", "hate");
        query.put("order", "1"); // 1 for ascending order
        mAPI.getQuestionList(query).enqueue(new Callback<List<Question>>() {
            @Override
            public void onResponse(Response<List<Question>> response, Retrofit retrofit) {
                List<Question> questions = response.body();
                if (questions != null) {
                    mQuestionAdapter.setQuestionList(questions);
                } else {
                    Log.e("Empty Response Body", "Null question list");
                }
            }

            @Override
            public void onFailure(Throwable t) {

            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    private void createSocketEventListener() {
        mSocket = mAPI.getSocket();
        mSocket.on("new post", new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String questionKey = args[0].toString();
                        mAPI.getQuestion(questionKey).enqueue(new Callback<Question>() {
                            @Override
                            public void onResponse(Response<Question> response, Retrofit retrofit) {
                                Question question = response.body();
                                if(question != null)
                                    mQuestionAdapter.insertQuestion(question, 0);
                            }

                            @Override
                            public void onFailure(Throwable t) {

                            }
                        });
                    }
                });
            }
        }).on("del post", new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String key = args[0].toString();
                        mQuestionAdapter.removeQuestion(key);
                    }
                });
            }
        }).on("like post", new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        JSONObject data = (JSONObject) args[0];
                        try {
                            String key = data.getString("id");
                            int numOfLikes = data.getInt("like");
                            int order = data.getInt("order");
                            mQuestionAdapter.likeQuestion(key, numOfLikes, order);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            return;
                        }
                    }
                });
            }
        }).on("dislike post", new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        JSONObject data = (JSONObject) args[0];
                        try {
                            String key = data.getString("id");
                            int numOfDislikes = data.getInt("dislike");
                            int order = data.getInt("order");
                            mQuestionAdapter.dislikeQuestion(key, numOfDislikes, order);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            return;
                        }
                    }
                });
            }
        });
        mSocket.connect();
        mSocket.emit("join", mRoomName);
    }
    @Override
    public void onResume(){
        super.onResume();
        Reset_Search();
    }

    public void emitLikeQuestion(String questionKey) {
        if(dbutil.contains(questionKey))
            return;
        mSocket.emit("like post", questionKey);
        dbutil.put(questionKey);
    }

    public void emitDislikeQuestion(String questionKey) {
        if(dbutil.contains(questionKey))
            return;
        mSocket.emit("dislike post", questionKey);
        dbutil.put(questionKey);
    }

    private void sendMessage() {
        EditText inputText = (EditText) findViewById(R.id.messageInput);
        String input = inputText.getText().toString();
        if (!input.equals("")) {
            // Create our 'model', a Chat object
            //Question question = new Question(input, mRoomName);

            Question question = new Question(input, mRoomName, mUsername, incognitoMode); // change Anonymous to the name of logged in user

            question.setImage(image);
            Button draw_button= (Button) findViewById(R.id.DrawButton);
            draw_button.setText("DRAW");
            image=null;
            // Create a new, auto-generated child of that chat location, and save our chat data there
            mAPI.saveQuesion(question).enqueue(new Callback<Question>() {
                @Override
                public void onResponse(Response<Question> response, Retrofit retrofit) {
                    Question question = response.body();
                    if(question != null) {
                        JSONObject jsonObject = new JSONObject();
                        try {
                            jsonObject.put("id", question.getKey());
                            jsonObject.put("room", mRoomName);
                        } catch (JSONException e) {}
                        mSocket.emit("new post", jsonObject);
                        mQuestionAdapter.insertQuestion(question, 0);
                    }
                    else {
                        Log.e("Empty Response Body", "Null Question");
                    }
                }

                @Override
                public void onFailure(Throwable t) {

                }
            });
            inputText.setText("");
        }
    }

    private void BeginDrawing(){
        Intent intent= new Intent(this, DrawActivity.class);
        intent.putExtra("RoomName",mRoomName);
        intent.putExtra("Username", mUsername);
        intent.putExtra("IncognitoMode",incognitoMode);
        startActivity(intent);
    }

    public void enterReply(String key) {
        Intent intent = new Intent(this, ReplyActivity.class);
        intent.putExtra("questionKey", key);
        intent.putExtra("Username", mUsername);
        intent.putExtra("IncognitoMode",incognitoMode);
        startActivity(intent);
    }

    public void Close(View view) {
        finish();
    }

    public void Search(View view) {
        Intent intent = new Intent(this, SearchActivity.class);
        intent.putExtra("Room Name", mRoomName);
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(final int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                long startTime = TimeDisplay.toTimestamp(data.getExtras().getString("StartTime"));
                long endTime = TimeDisplay.toTimestamp(data.getExtras().getString("EndTime"));
                String content = data.getExtras().getString("Content");
                final Map<String, String> query = new ArrayMap<>();
                query.put("roomName", mRoomName);
                query.put("startTime", String.valueOf(startTime));
                query.put("endTime", String.valueOf(endTime));
                query.put("content", content);
                mAPI.getQuestionList(query).enqueue(new Callback<List<Question>>() {
                    @Override
                    public void onResponse(Response<List<Question>> response, Retrofit retrofit) {
                        List<Question> questions = response.body();
                        if (questions != null) {
                            mQuestionAdapter.setQuestionList(questions);
                        } else {
                            Log.e("Empty Response Body", "Null Question List");
                        }
                    }

                    @Override
                    public void onFailure(Throwable t) {

                    }
                });
            }
        }
    }

    @Override
    public void startActivity(Intent intent) {
        if (TextUtils.equals(intent.getAction(), Intent.ACTION_VIEW)) {
            Uri uri = intent.getData();
            //strip off hashtag from the URI
            String tag=uri.toString();
            //System.out.println(tag.substring(3));
            Map<String, String> query = new ArrayMap<>();
            query.put("roomName", mRoomName);
            query.put("content", tag.substring(3));
            mAPI.getQuestionList(query).enqueue(new Callback<List<Question>>() {
                @Override
                public void onResponse(Response<List<Question>> response, Retrofit retrofit) {
                    List<Question> questions = response.body();
                    if (questions != null) {
                        mQuestionAdapter.setQuestionList(questions);
                    } else {
                        Log.e("Empty Response Body", "Null Question List");
                    }
                }

                @Override
                public void onFailure(Throwable t) {

                }
            });
        }
        else {
            super.startActivity(intent);
        }
    }

    public void setIncognitoMode(boolean b) { incognitoMode = b;}

};



