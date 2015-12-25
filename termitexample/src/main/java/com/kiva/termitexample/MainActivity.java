package com.kiva.termitexample;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.kiva.termit.TermCaller;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private String handle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        TermCaller.Builder builder = new TermCaller.Builder();

        if (handle == null) {
            builder.newWindow("TermItExample", "echo hello world");
        } else {
            builder.appendTo(handle, "echo append");
        }

        Intent i = builder.build().getIntent();
        startActivityForResult(i, 123);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 123) {
            handle = TermCaller.getResult(data);
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
