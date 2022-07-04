package com.kassem.movieappkasspart2;

import android.content.Context;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;
import com.kassem.movieappkasspart2.model.MoviesClass;
import com.kassem.movieappkasspart2.utilities.NetworkUtils;

import java.util.List;

public class MovieAdapter extends RecyclerView.Adapter<MovieAdapter.MovieViewHolder> {
    private static final String TAG = MovieAdapter.class.getSimpleName();

    private List<MoviesClass> tMovieItemList;
    private final Context tContext;
    final private ListItemClickListener tOnClickListener;

    public interface ListItemClickListener {
        void OnListItemClick(MoviesClass movieItem);

        Parcelable onSaveInstanceState();

        void onRestoreInstanceState(Parcelable state);
    }

    public MovieAdapter(List<MoviesClass> movieItemList, ListItemClickListener listener, Context context) {

        tMovieItemList = movieItemList;

       tOnClickListener = listener;
        tContext = context;
    }

    @NonNull
    @Override
    public MovieViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        int layoutIdForListItem = R.layout.movie;
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(layoutIdForListItem, parent, false);
        return new MovieViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MovieViewHolder holder, int position) {
        holder.bind(position);
    }

    @Override
    public int getItemCount() {
        return tMovieItemList == null ? 0 : tMovieItemList.size();
    }

    public void setMovieData(List<MoviesClass> movieItemList) {
        tMovieItemList = movieItemList;
        notifyDataSetChanged();
    }

    public class MovieViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView listMovieItemView;

        public MovieViewHolder(View itemView) {
            super(itemView);

            listMovieItemView = itemView.findViewById(R.id.iv_item_poster);
            itemView.setOnClickListener(this);
        }

        void bind(int listIndex) {
            MoviesClass movieItem = tMovieItemList.get(listIndex);
            listMovieItemView = itemView.findViewById(R.id.iv_item_poster);
            String posterPathURL = NetworkUtils.buildPosterUrl(movieItem.getImage());
            try {
                Picasso.with(tContext)
                        .load(posterPathURL)
                        .placeholder(R.mipmap.ic_launcher)
                        .error(R.mipmap.ic_launcher)
                        .into(listMovieItemView);
            } catch (Exception ex) {
                Log.e(TAG, ex.getMessage());
            }
        }

        @Override
        public void onClick(View view) {
            int clickedPosition = getAdapterPosition();
            tOnClickListener.OnListItemClick(tMovieItemList.get(clickedPosition));
        }
    }

}
