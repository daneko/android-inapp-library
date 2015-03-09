package jp.daneko.example;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import fj.data.Option;

import rx.Observable;
import rx.subjects.PublishSubject;

import lombok.Getter;

import com.github.daneko.android.iab.IabContext;
import com.github.daneko.android.iab.model.ActivityResults;

public class MyActivity extends Activity implements IabContext {

    @Getter
    private PublishSubject<ActivityResults> resultsPublish = PublishSubject.create();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new MyFragment())
                    .commit();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        final ActivityResults result = ActivityResults.of(requestCode, resultCode, Option.fromNull(data));
        resultsPublish.onNext(result);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.my, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Activity getActivity() {
        return this;
    }

    @Override
    public Observable<ActivityResults> getActivityResultObservable() {
        return getResultsPublish().asObservable();
    }

}
