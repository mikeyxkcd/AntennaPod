package de.danoeh.antennapod.activity;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.mobeta.android.dslv.DragSortListView;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.asynctask.ImageLoader;
import de.danoeh.antennapod.feed.EventDistributor;
import de.danoeh.antennapod.feed.FeedItem;
import de.danoeh.antennapod.feed.FeedManager;
import de.danoeh.antennapod.preferences.UserPreferences;

public class OrganizeQueueActivity extends SherlockListActivity {
	private static final String TAG = "OrganizeQueueActivity";

	private static final int MENU_ID_ACCEPT = 2;

	private OrganizeAdapter adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setTheme(UserPreferences.getTheme());
		super.onCreate(savedInstanceState);
		setContentView(R.layout.organize_queue);

		DragSortListView listView = (DragSortListView) getListView();
		listView.setDropListener(dropListener);
		listView.setRemoveListener(removeListener);

		adapter = new OrganizeAdapter(this);
		setListAdapter(adapter);
	}

	@Override
	protected void onPause() {
		super.onPause();
		EventDistributor.getInstance().unregister(contentUpdate);
	}

	@Override
	protected void onResume() {
		super.onResume();
		EventDistributor.getInstance().register(contentUpdate);
	}

	private EventDistributor.EventListener contentUpdate = new EventDistributor.EventListener() {

		@Override
		public void update(EventDistributor eventDistributor, Integer arg) {
			if (((EventDistributor.QUEUE_UPDATE | EventDistributor.FEED_LIST_UPDATE) & arg) != 0) {
				if (adapter != null) {
					adapter.notifyDataSetChanged();
				}
			}
		}
	};

	private DragSortListView.DropListener dropListener = new DragSortListView.DropListener() {

		@Override
		public void drop(int from, int to) {
			FeedManager manager = FeedManager.getInstance();
			manager.moveQueueItem(OrganizeQueueActivity.this, from, to, false);
			adapter.notifyDataSetChanged();
		}
	};

	private DragSortListView.RemoveListener removeListener = new DragSortListView.RemoveListener() {

		@Override
		public void remove(int which) {
			FeedManager manager = FeedManager.getInstance();
			manager.removeQueueItem(OrganizeQueueActivity.this,
					(FeedItem) getListAdapter().getItem(which));
		}
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		TypedArray drawables = obtainStyledAttributes(new int[] { R.attr.navigation_accept });
		menu.add(Menu.NONE, MENU_ID_ACCEPT, Menu.NONE, R.string.confirm_label)
				.setIcon(drawables.getDrawable(0))
				.setShowAsAction(
						MenuItem.SHOW_AS_ACTION_IF_ROOM
								| MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_ID_ACCEPT:
			finish();
			return true;
		default:
			return false;
		}
	}

	/**
	 * WARNING: If the PlaybackService is playing an episode from the queue,
	 * this list adapter will ignore the first item in the list to make sure
	 * that the position of the first queue item cannot be changed.
	 */
	private static class OrganizeAdapter extends BaseAdapter {

		private Context context;
		private FeedManager manager = FeedManager.getInstance();

		public OrganizeAdapter(Context context) {
			super();
			this.context = context;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			Holder holder;
			final FeedItem item = getItem(position);

			if (convertView == null) {
				holder = new Holder();
				LayoutInflater inflater = (LayoutInflater) context
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = inflater.inflate(
						R.layout.organize_queue_listitem, null);
				holder.title = (TextView) convertView
						.findViewById(R.id.txtvTitle);
				holder.feedTitle = (TextView) convertView
						.findViewById(R.id.txtvFeedname);

				holder.feedImage = (ImageView) convertView
						.findViewById(R.id.imgvFeedimage);
				convertView.setTag(holder);
			} else {
				holder = (Holder) convertView.getTag();
			}

			holder.title.setText(item.getTitle());
			holder.feedTitle.setText(item.getFeed().getTitle());

			holder.feedImage.setTag(item.getImageLoaderCacheKey());
			ImageLoader.getInstance().loadThumbnailBitmap(
					item,
					holder.feedImage,
					(int) convertView.getResources().getDimension(
							R.dimen.thumbnail_length));

			return convertView;
		}

		static class Holder {
			TextView title;
			TextView feedTitle;
			ImageView feedImage;
		}

		@Override
		public int getCount() {
			int queueSize = manager.getQueueSize(true);
			return queueSize;
		}

		@Override
		public FeedItem getItem(int position) {
			return manager.getQueueItemAtIndex(position, true);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

	}

}
