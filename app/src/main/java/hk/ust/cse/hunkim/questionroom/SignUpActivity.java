package hk.ust.cse.hunkim.questionroom;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import hk.ust.cse.hunkim.questionroom.question.ResponseResult;
import hk.ust.cse.hunkim.questionroom.question.User;
import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;

public class SignUpActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        findViewById(R.id.signup_button2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TryToSignUp();
            }
        });
    }

public void TryToSignUp() {
    String username = ((EditText) findViewById(R.id.username2)).getText().toString();
    String password = ((EditText) findViewById(R.id.password2)).getText().toString();
    APIService service = RESTfulAPI.getInstance().getService();
    User user = new User(username, password);
    service.userAuth(user, "signup").enqueue(new Callback<ResponseResult>() {
        @Override
        public void onResponse(Response<ResponseResult> response, Retrofit retrofit) {
            ResponseResult result = response.body();
            if (result != null && result.getResult() == true) {
                // sign up success
            }
        }

        @Override
        public void onFailure(Throwable t) {

        }
    });
}
}
