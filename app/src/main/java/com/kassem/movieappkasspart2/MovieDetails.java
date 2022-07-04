package com.kassem.movieappkasspart2;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import com.squareup.picasso.Picasso;
import com.kassem.movieappkasspart2.database.FavoriteMovie;
import com.kassem.movieappkasspart2.database.MovieDatabase;
import com.kassem.movieappkasspart2.model.MoviesClass;
import com.kassem.movieappkasspart2.model.ReviewClass;
import com.kassem.movieappkasspart2.model.TrailerClass;
import com.kassem.movieappkasspart2.utilities.JsonUtils;
import com.kassem.movieappkasspart2.utilities.NetworkUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

public class MovieDetails extends AppCompatActivity  implements TrailerAdapter.ListItemClickListener{
    private static final String TAG = MovieDetails.class.getSimpleName();
    private MoviesClass movItem;
    private ArrayList<ReviewClass> revuList;
    private ArrayList<TrailerClass> traList;

    private RecyclerView rTrailerRecyclerView;
    private TrailerAdapter rTrailerAdapter;
    private RecyclerView.LayoutManager rLayoutManager;

    private MovieDatabase rDb;
    private ImageView rFavButton;
    private Boolean isFav = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        Intent intent = getIntent();
        if (intent == null) {
            closeOnError("Intent is null");
        }

        movItem = (MoviesClass) intent.getSerializableExtra("movieItem");
        if (movItem == null) {
            closeOnError(getString(R.string.Error_NoMovieData));
            return;
        }

        rTrailerRecyclerView = findViewById(R.id.rv_trailers);
        rTrailerAdapter = new TrailerAdapter(this, traList, this);
        rTrailerRecyclerView.setAdapter(rTrailerAdapter);
        rLayoutManager = new LinearLayoutManager(this);
       rTrailerRecyclerView.setLayoutManager(rLayoutManager);

        rFavButton = findViewById(R.id.iv_favButton);
        rDb = MovieDatabase.getInstance(getApplicationContext());

        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                final FavoriteMovie fmov = rDb.movieDao().loadMovieById(Integer.parseInt(movItem.getId()));
                setFavorite((fmov != null)? true : false);
            }
        });

        getMoreDetails(movItem.getId());

    }

    private void setFavorite(Boolean fav){
        if (fav) {
            isFav = true;
            rFavButton.setImageResource(R.drawable.ic_favorite_solid_24dp);
        } else {
            isFav = false;
            rFavButton.setImageResource(R.drawable.ic_favorite_border_black_24dp);
        }
    }

    private static class SearchURLs {
        URL reviewSearchUrl;
        URL trailerSearchUrl;
        SearchURLs(URL reviewSearchUrl, URL trailerSearchUrl){
            this.reviewSearchUrl = reviewSearchUrl;
            this.trailerSearchUrl = trailerSearchUrl;
        }
    }
    private static class ResultsStrings {
        String reviewString;
        String trailerString;
        ResultsStrings(String reviewString, String trailerString){
            this.reviewString = reviewString;
            this.trailerString = trailerString;
        }
    }
    private void getMoreDetails(String id) {
        String reviewQuery = id + File.separator + "reviews";
        String trailerQuery = id + File.separator + "videos";
        SearchURLs searchURLs = new SearchURLs(
                NetworkUtils.buildUrl(reviewQuery, getText(R.string.api_key).toString()),
                NetworkUtils.buildUrl(trailerQuery, getText(R.string.api_key).toString())
                );
        new ReviewsQueryTask().execute(searchURLs);
    }


    // AsyncTask to perform query
    public class ReviewsQueryTask extends AsyncTask<SearchURLs, Void, ResultsStrings> {
        @Override
        protected ResultsStrings doInBackground(SearchURLs... params) {
            URL reviewsearchUrl = params[0].reviewSearchUrl;
            URL trailersearchUrl = params[0].trailerSearchUrl;

            String reviewResults = null;
            try {
                reviewResults = NetworkUtils.getResponseFromHttpUrl(reviewsearchUrl);
                revuList = JsonUtils.parseReviewsJson(reviewResults);
            } catch (IOException e) {
                e.printStackTrace();
            }

            String trailerResults = null;
            try {
                trailerResults = NetworkUtils.getResponseFromHttpUrl(trailersearchUrl);
                traList = JsonUtils.parseTrailersJson(trailerResults);
            } catch (IOException e) {
                e.printStackTrace();
            }

            ResultsStrings results = new ResultsStrings(reviewResults,trailerResults);

            return results;
        }

        @Override
        protected void onPostExecute(ResultsStrings results) {
            String searchResults = results.reviewString;
            if (searchResults != null && !searchResults.equals("")) {
                revuList = JsonUtils.parseReviewsJson(searchResults);
                populateDetails();
            }
        }
    }

    private void watchYoutubeVideo(String id){
        Intent appIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:" + id));
        Intent webIntent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("http://www.youtube.com/watch?v=" + id));
        webIntent.putExtra("finish_on_ended", true);
        try {
            startActivity(appIntent);
        } catch (ActivityNotFoundException ex) {
            startActivity(webIntent);
        }
    }

    @Override
    public void OnListItemClick(TrailerClass trailerItem) {
        Log.d(TAG,trailerItem.getKey());
        watchYoutubeVideo(trailerItem.getKey());
    }

    private void populateDetails() {

        ((TextView)findViewById(R.id.tv_title)).setText(movItem.getTitle());
        ((TextView)findViewById(R.id.tv_header_rating)).append(" ("+movItem.getVote()+"/10)");
        ((RatingBar)findViewById(R.id.rbv_user_rating)).setRating(Float.parseFloat(movItem.getVote()));
        ((TextView)findViewById(R.id.tv_release_date)).setText(movItem.getReleaseDate());
        ((TextView)findViewById(R.id.tv_synopsis)).setText(movItem.getSynopsis());

        // Favorite
        rFavButton.setOnClickListener(new View.OnClickListener() {
            //@Override
            public void onClick(View v) {
                final FavoriteMovie mov = new FavoriteMovie(
                        Integer.parseInt(movItem.getId()),
                        movItem.getTitle(),
                        movItem.getReleaseDate(),
                        movItem.getVote(),
                        movItem.getPopularity(),
                        movItem.getSynopsis(),
                        movItem.getImage(),
                        movItem.getBackdrop()
                );
                AppExecutors.getInstance().diskIO().execute(new Runnable() {
                    @Override
                    public void run() {
                        if (isFav) {
                            // delete item
                            rDb.movieDao().deleteMovie(mov);
                        } else {
                            // insert item
                            rDb.movieDao().insertMovie(mov);
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setFavorite(!isFav);
                            }
                        });
                    }

                });
            }
        });



        // Trailers
        rTrailerAdapter.setTrailerData(traList);

        // Reviews
        ((TextView)findViewById(R.id.tv_reviews)).setText("");
        for(int i=0; i<revuList.size(); i++) {
            ((TextView)findViewById(R.id.tv_reviews)).append("\n");
            ((TextView)findViewById(R.id.tv_reviews)).append(revuList.get(i).getContent());
            ((TextView)findViewById(R.id.tv_reviews)).append("\n\n");
            ((TextView)findViewById(R.id.tv_reviews)).append(" - Reviewed by ");
            ((TextView)findViewById(R.id.tv_reviews)).append(revuList.get(i).getAuthor());
            ((TextView)findViewById(R.id.tv_reviews)).append("\n\n--------------\n");
        }

        String backdropPathURL = NetworkUtils.buildPosterUrl(movItem.getBackdrop());

        try {
            Picasso.with(this)
                    .load(backdropPathURL)
                    .placeholder(R.mipmap.ic_launcher)
                    .error(R.mipmap.ic_launcher)
                    .into((ImageView)this.findViewById(R.id.iv_backdrop));
        } catch (Exception ex) {
            Log.e(TAG, ex.getMessage());
        }

        String imagePathURL = NetworkUtils.buildPosterUrl(movItem.getImage());

        try {
            Picasso.with(this)
                    .load(imagePathURL)
                    .placeholder(R.mipmap.ic_launcher)
                    .error(R.mipmap.ic_launcher)
                    .into((ImageView)this.findViewById(R.id.iv_image));
        } catch (Exception ex) {
            Log.e(TAG, ex.getMessage());
        }

    }

    private void closeOnError(String msg) {
        finish();
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
