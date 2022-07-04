package com.kassem.movieappkasspart2;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.kassem.movieappkasspart2.database.FavoriteMovie;
import com.kassem.movieappkasspart2.model.MoviesClass;
import com.kassem.movieappkasspart2.utilities.JsonUtils;
import com.kassem.movieappkasspart2.utilities.NetworkUtils;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements MovieAdapter.ListItemClickListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    int mScrollPosition=4;

    private static final String POP = "popular";
    private static final String RATE = "top_rated";
    private static final String FAVOR = "favorite";
    private static String currentSort = POP;

    private ArrayList<MoviesClass> movList;

    private RecyclerView bMovieRecyclerView;
    private MovieAdapter bMovieAdapter;

    private List<FavoriteMovie> fav_Mov;
    private Parcel superState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bMovieRecyclerView = (RecyclerView) findViewById(R.id.rv_main);
        GridLayoutManager layoutManager = new GridLayoutManager(this, 3);
        bMovieRecyclerView.setLayoutManager(layoutManager);
        bMovieRecyclerView.setHasFixedSize(true);

       bMovieAdapter = new MovieAdapter(movList, this, this);
        bMovieRecyclerView.setAdapter(bMovieAdapter);

        fav_Mov = new ArrayList<FavoriteMovie>();

        setTitle(getString(R.string.app_name) + " - Popular");

        setupViewModel();
    }

    private void loadMovies() {
        makeMovieSearchQuery();
       }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_sort_popular && !currentSort.equals(POP)) {
            ClearMovieItemList();
            currentSort = POP;
            setTitle(getString(R.string.app_name) + " - Popular");
            loadMovies();
            return true;
        }
        if (id == R.id.action_sort_top_rated && !currentSort.equals(RATE)) {
            ClearMovieItemList();
            currentSort = RATE;
            setTitle(getString(R.string.app_name) + " - Top rated");
            loadMovies();
            return true;
        }
        if (id == R.id.action_sort_favorite && !currentSort.equals(FAVOR)) {
            ClearMovieItemList();
            currentSort = FAVOR;
            setTitle(getString(R.string.app_name) + " - Favorite");
            loadMovies();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void ClearMovieItemList() {
        if (movList != null) {
            movList.clear();
        } else {
            movList = new ArrayList<MoviesClass>();
        }
    }

    private void makeMovieSearchQuery() {
        if (currentSort.equals(FAVOR)) {
            ClearMovieItemList();
            for (int i = 0; i< fav_Mov.size(); i++) {
                MoviesClass mov = new MoviesClass(
                        String.valueOf(fav_Mov.get(i).getId()),
                        fav_Mov.get(i).getTitle(),
                        fav_Mov.get(i).getReleaseDate(),
                        fav_Mov.get(i).getVote(),
                        fav_Mov.get(i).getPopularity(),
                        fav_Mov.get(i).getSynopsis(),
                        fav_Mov.get(i).getImage(),
                        fav_Mov.get(i).getBackdrop()
                );
                movList.add( mov );
            }
           bMovieAdapter.setMovieData(movList);
        } else {
            String movieQuery = currentSort;
            URL movieSearchUrl = NetworkUtils.buildUrl(movieQuery, getText(R.string.api_key).toString());
            new MoviesQueryTask().execute(movieSearchUrl);
        }
    }

    public class MoviesQueryTask extends AsyncTask<URL, Void, String> {

        @Override
        protected String doInBackground(URL... params) {
            URL searchUrl = params[0];
            String searchResults = null;
            try {
                searchResults = NetworkUtils.getResponseFromHttpUrl(searchUrl);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return searchResults;
        }

        @Override
        protected void onPostExecute(String searchResults) {
            if (searchResults != null && !searchResults.equals("")) {
                movList = JsonUtils.parseMoviesJson(searchResults);
                bMovieAdapter.setMovieData(movList);
            }
        }
    }

    private void setupViewModel() {
        MainViewModel viewModel = ViewModelProviders.of(this).get(MainViewModel.class);
        viewModel.getMovies().observe(this, new Observer<List<FavoriteMovie>>() {
            @Override
            public void onChanged(@Nullable List<FavoriteMovie> favs) {
                if(favs.size()>0) {
                    fav_Mov.clear();
                    fav_Mov = favs;
                }
                for (int i=0; i<fav_Mov.size(); i++) {
                    Log.d(TAG,fav_Mov.get(i).getTitle());
                }
                loadMovies();
            }
        });
    }
    public void OnListItemClick(MoviesClass movieItem) {
        Intent myIntent = new Intent(this, MovieDetails.class);
        myIntent.putExtra("movieItem", movieItem);
        startActivity(myIntent);
    }
    @Override
    public Parcelable onSaveInstanceState() {

        RecyclerView.LayoutManager layoutManager = getLayoutManager();

        if(layoutManager != null && layoutManager instanceof LinearLayoutManager){
            mScrollPosition = ((LinearLayoutManager) layoutManager).findFirstVisibleItemPosition();
        }

        SavedState newState = new SavedState(superState);
        newState.mScrollPosition = mScrollPosition;
        return newState;
    }

    private RecyclerView.LayoutManager getLayoutManager() {
        return null;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        super.onRestoreInstanceState((Bundle) state);
        if(state != null && state instanceof SavedState){
            int mScrollPosition = ((SavedState) state).mScrollPosition;
            RecyclerView.LayoutManager layoutManager = getLayoutManager();
            if(layoutManager != null){
                int count = layoutManager.getItemCount();
                if(mScrollPosition != RecyclerView.NO_POSITION && mScrollPosition < count){
                    layoutManager.scrollToPosition(mScrollPosition);
                }
            }
        }
    }

    static class SavedState extends android.view.View.BaseSavedState {
        public int mScrollPosition;
        SavedState(Parcel in) {
            super(in);
            mScrollPosition = in.readInt();
        }
        SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(mScrollPosition);
        }
        public static final Parcelable.Creator<SavedState> CREATOR
                = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}
