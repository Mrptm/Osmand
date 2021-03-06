package net.osmand.plus.mapmarkers;

import android.os.Bundle;

import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.FavouritesDbHelper.FavoriteGroup;
import net.osmand.plus.FavouritesDbHelper.FavoritesListener;
import net.osmand.plus.mapmarkers.adapters.FavouritesGroupsAdapter;
import net.osmand.plus.mapmarkers.adapters.GroupsAdapter;

public class AddFavouritesGroupBottomSheetDialogFragment extends AddGroupBottomSheetDialogFragment {

	private FavouritesDbHelper favouritesDbHelper;
	private FavoritesListener favoritesListener;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		favouritesDbHelper = getMyApplication().getFavorites();
	}

	@Override
	public void onPause() {
		super.onPause();
		if (favoritesListener != null) {
			favouritesDbHelper.removeListener(favoritesListener);
			favoritesListener = null;
		}
	}

	@Override
	public GroupsAdapter createAdapter() {
		if (!favouritesDbHelper.isFavoritesLoaded()) {
			favouritesDbHelper.addListener(favoritesListener = new FavoritesListener() {
				@Override
				public void onFavoritesLoaded() {
					if (adapter != null) {
						adapter.notifyDataSetChanged();
					}
				}
			});
		}
		return new FavouritesGroupsAdapter(getContext(), favouritesDbHelper.getFavoriteGroups());
	}

	@Override
	protected void onItemClick(int position) {
		FavoriteGroup group = favouritesDbHelper.getFavoriteGroups().get(position - 1);
		if (!group.visible) {
			favouritesDbHelper.editFavouriteGroup(group, group.name, group.color, true);
		}
		getMyApplication().getMapMarkersHelper().addOrEnableGroup(group);
		dismiss();
	}
}
