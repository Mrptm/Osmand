package net.osmand.plus.mapcontextmenu.other;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.ValueHolder;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.GeocodingLookupService;
import net.osmand.plus.GeocodingLookupService.AddressLookupRequest;
import net.osmand.plus.IconsCache;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.TargetPointsHelper.TargetPoint;
import net.osmand.plus.activities.FavoritesListFragment.FavouritesAdapter;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.ShowRouteInfoActivity;
import net.osmand.plus.activities.actions.AppModeDialog;
import net.osmand.plus.activities.search.SearchAddressActivity;
import net.osmand.plus.dialogs.FavoriteDialogs;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.routing.RouteDirectionInfo;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.routing.RoutingHelper.IRouteInformationListener;
import net.osmand.plus.views.MapControlsLayer;
import net.osmand.plus.views.OsmandMapTileView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MapRouteInfoMenu implements IRouteInformationListener {
	public static int directionInfo = -1;
	public static boolean controlVisible = false;
	private final MapContextMenu contextMenu;
	private final RoutingHelper routingHelper;
	private OsmandMapTileView mapView;
	private GeocodingLookupService geocodingLookupService;
	private boolean selectFromMapTouch;
	private boolean selectFromMapForTarget;

	private boolean showMenu = false;
	private boolean visible;
	private MapActivity mapActivity;
	private MapControlsLayer mapControlsLayer;
	public static final String TARGET_SELECT = "TARGET_SELECT";
	private boolean nightMode;
	private boolean switched;

	private AddressLookupRequest startPointRequest;
	private AddressLookupRequest targetPointRequest;

	private static final long SPINNER_MY_LOCATION_ID = 1;
	private static final long SPINNER_FAV_ID = 2;
	private static final long SPINNER_MAP_ID = 3;
	private static final long SPINNER_ADDRESS_ID = 4;
	private static final long SPINNER_START_ID = 5;
	private static final long SPINNER_FINISH_ID = 6;
	private static final long SPINNER_HINT_ID = 100;

	public MapRouteInfoMenu(MapActivity mapActivity, MapControlsLayer mapControlsLayer) {
		this.mapActivity = mapActivity;
		this.mapControlsLayer = mapControlsLayer;
		contextMenu = mapActivity.getContextMenu();
		routingHelper = mapActivity.getRoutingHelper();
		mapView = mapActivity.getMapView();
		routingHelper.addListener(this);
		geocodingLookupService = mapActivity.getMyApplication().getGeocodingLookupService();
	}

	public boolean onSingleTap(PointF point, RotatedTileBox tileBox) {
		if (selectFromMapTouch) {
			LatLon latlon = tileBox.getLatLonFromPixel(point.x, point.y);
			selectFromMapTouch = false;
			if (selectFromMapForTarget) {
				getTargets().navigateToPoint(latlon, true, -1);
			} else {
				getTargets().setStartPoint(latlon, true, null);
			}
			contextMenu.showMinimized(latlon, null, null);
			show();
			return true;
		}
		return false;
	}

	private void cancelStartPointAddressRequest() {
		if (startPointRequest != null) {
			geocodingLookupService.cancel(startPointRequest);
			startPointRequest = null;
		}
	}

	private void cancelTargetPointAddressRequest() {
		if (targetPointRequest != null) {
			geocodingLookupService.cancel(targetPointRequest);
			targetPointRequest = null;
		}
	}

	public void setVisible(boolean visible) {
		if (visible) {
			if (showMenu) {
				show();
				showMenu = false;
			}
			controlVisible = true;
		} else {
			hide();
			controlVisible = false;
		}
	}


	public void showHideMenu() {
		if (isVisible()) {
			hide();
		} else {
			show();
		}
	}

	public void updateMenu() {
		WeakReference<MapRouteInfoMenuFragment> fragmentRef = findMenuFragment();
		if (fragmentRef != null)
			fragmentRef.get().updateInfo();
	}

	public void updateFromIcon() {
		WeakReference<MapRouteInfoMenuFragment> fragmentRef = findMenuFragment();
		if (fragmentRef != null)
			fragmentRef.get().updateFromIcon();
	}

	public void updateInfo(final View main) {
		nightMode = mapActivity.getMyApplication().getDaynightHelper().isNightModeForMapControls();
		updateViaView(main);
		updateFromSpinner(main);
		updateToSpinner(main);
		updateApplicationModes(main);
		mapControlsLayer.updateRouteButtons(main, true);
		boolean addButtons = routingHelper.isRouteCalculated();
		if (addButtons) {
			updateRouteButtons(main);
		} else {
			updateRouteCalcProgress(main);
		}
	}

	private void updateRouteCalcProgress(final View main) {
		TargetPointsHelper targets = getTargets();
		if (targets.hasTooLongDistanceToNavigate()) {
			main.findViewById(R.id.dividerToDropDown).setVisibility(View.VISIBLE);
			main.findViewById(R.id.RouteInfoControls).setVisibility(View.VISIBLE);
			TextView textView = (TextView) main.findViewById(R.id.InfoTextView);
			ImageView iconView = (ImageView) main.findViewById(R.id.InfoIcon);
			main.findViewById(R.id.Prev).setVisibility(View.GONE);
			main.findViewById(R.id.Next).setVisibility(View.GONE);
			main.findViewById(R.id.InfoIcon).setVisibility(View.GONE);
			main.findViewById(R.id.DurationIcon).setVisibility(View.GONE);
			main.findViewById(R.id.InfoDistance).setVisibility(View.GONE);
			main.findViewById(R.id.InfoDuration).setVisibility(View.GONE);
			textView.setText(R.string.route_is_too_long);
			textView.setVisibility(View.VISIBLE);
			iconView.setImageDrawable(mapActivity.getMyApplication().getIconsCache().getContentIcon(R.drawable.ic_warning, isLight()));
		} else {
			main.findViewById(R.id.dividerToDropDown).setVisibility(View.GONE);
			main.findViewById(R.id.RouteInfoControls).setVisibility(View.GONE);
		}
	}

	private void updateApplicationModes(final View parentView) {
		final OsmandSettings settings = mapActivity.getMyApplication().getSettings();
		ApplicationMode am = settings.APPLICATION_MODE.get();
		final Set<ApplicationMode> selected = new HashSet<>();
		selected.add(am);
		ViewGroup vg = (ViewGroup) parentView.findViewById(R.id.app_modes);
		vg.removeAllViews();
		AppModeDialog.prepareAppModeView(mapActivity, selected, false,
				vg, true, true, new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (selected.size() > 0) {
							ApplicationMode next = selected.iterator().next();
							settings.APPLICATION_MODE.set(next);
							updateMenu();
							mapActivity.getRoutingHelper().recalculateRouteDueToSettingsChange();
						}
					}
				});
	}

	private void updateViaView(final View parentView) {
		String via = generateViaDescription();
		View viaLayout = parentView.findViewById(R.id.ViaLayout);
		if (via.length() == 0) {
			viaLayout.setVisibility(View.GONE);
			parentView.findViewById(R.id.viaLayoutDivider).setVisibility(View.GONE);
		} else {
			viaLayout.setVisibility(View.VISIBLE);
			parentView.findViewById(R.id.viaLayoutDivider).setVisibility(View.VISIBLE);
			((TextView) parentView.findViewById(R.id.ViaView)).setText(via);
		}

		viaLayout.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (getTargets().checkPointToNavigateShort()) {
					mapActivity.getMapActions().openIntermediatePointsDialog();
				}
			}
		});

		ImageView viaIcon = (ImageView) parentView.findViewById(R.id.viaIcon);
		if (isLight()) {
			viaIcon.setImageDrawable(getIconOrig(R.drawable.widget_intermediate_day));
		} else {
			viaIcon.setImageDrawable(getIconOrig(R.drawable.widget_intermediate_night));
		}
	}

	private void updateToSpinner(final View parentView) {
		final Spinner toSpinner = setupToSpinner(parentView);
		toSpinner.setClickable(false);
		toSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				if (id == SPINNER_FAV_ID) {
					selectFavorite(parentView, true);
				} else if (id == SPINNER_MAP_ID) {
					selectOnScreen(true);
				} else if (id == SPINNER_ADDRESS_ID) {
					Intent intent = new Intent(mapActivity, SearchAddressActivity.class);
					intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
					intent.putExtra(TARGET_SELECT, true);
					mapActivity.startActivityForResult(intent, MapControlsLayer.REQUEST_ADDRESS_SELECT);
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});

		parentView.findViewById(R.id.ToLayout).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				toSpinner.performClick();
			}
		});

		updateToIcon(parentView);

		ImageView toDropDownIcon = (ImageView) parentView.findViewById(R.id.toDropDownIcon);
		toDropDownIcon.setImageDrawable(mapActivity.getMyApplication().getIconsCache().getContentIcon(R.drawable.ic_action_arrow_drop_down, isLight()));
	}

	private void updateToIcon(View parentView) {
		ImageView toIcon = (ImageView) parentView.findViewById(R.id.toIcon);
		if (isLight()) {
			toIcon.setImageDrawable(getIconOrig(R.drawable.widget_target_day));
		} else {
			toIcon.setImageDrawable(getIconOrig(R.drawable.widget_target_night));
		}
	}

	private void updateFromSpinner(final View parentView) {
		final TargetPointsHelper targets = getTargets();
		final Spinner fromSpinner = setupFromSpinner(parentView);
		fromSpinner.setClickable(false);
		fromSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				if (id == SPINNER_MY_LOCATION_ID) {
					if (targets.getPointToStart() != null) {
						targets.clearStartPoint(true);
					}
					updateFromIcon(parentView);
				} else if (id == SPINNER_FAV_ID) {
					selectFavorite(parentView, false);
				} else if (id == SPINNER_MAP_ID) {
					selectOnScreen(false);
				} else if (id == SPINNER_ADDRESS_ID) {
					Intent intent = new Intent(mapActivity, SearchAddressActivity.class);
					intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
					intent.putExtra(TARGET_SELECT, false);
					mapActivity.startActivityForResult(intent, MapControlsLayer.REQUEST_ADDRESS_SELECT);
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});

		parentView.findViewById(R.id.FromLayout).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				fromSpinner.performClick();
			}
		});

		updateFromIcon(parentView);

		ImageView fromDropDownIcon = (ImageView) parentView.findViewById(R.id.fromDropDownIcon);
		fromDropDownIcon.setImageDrawable(mapActivity.getMyApplication().getIconsCache().getContentIcon(R.drawable.ic_action_arrow_drop_down, isLight()));
	}

	@SuppressWarnings("deprecation")
	public void updateFromIcon(View parentView) {
		TargetPointsHelper targets = getTargets();
		ImageView fromIcon = (ImageView) parentView.findViewById(R.id.fromIcon);
		if (targets.getPointToStart() == null) {
			ApplicationMode appMode = mapActivity.getMyApplication().getSettings().getApplicationMode();
			fromIcon.setImageDrawable(mapActivity.getResources().getDrawable(appMode.getResourceLocationDay()));
		} else {
			fromIcon.setImageDrawable(mapActivity.getMyApplication().getIconsCache().getContentIcon(R.drawable.ic_action_marker_dark, isLight()));
		}
	}

	protected void selectOnScreen(boolean target) {
		selectFromMapTouch = true;
		selectFromMapForTarget = target;
		hide();
	}

	public void selectAddress(String name, LatLon l, final boolean target) {
		PointDescription pd = new PointDescription(PointDescription.POINT_TYPE_ADDRESS, name);
		if (target) {
			getTargets().navigateToPoint(l, true, -1, pd);
		} else {
			getTargets().setStartPoint(l, true, pd);
		}
		updateMenu();
	}

	protected void selectFavorite(final View parentView, final boolean target) {
		final FavouritesAdapter favouritesAdapter = new FavouritesAdapter(mapActivity, mapActivity.getMyApplication()
				.getFavorites().getFavouritePoints(), false);
		Dialog[] dlgHolder = new Dialog[1];
		OnItemClickListener click = getOnClickListener(target, favouritesAdapter, dlgHolder);
		OnDismissListener dismissListener = new DialogInterface.OnDismissListener() {

			@Override
			public void onDismiss(DialogInterface dialog) {
				if (target) {
					setupToSpinner(parentView);
				} else {
					setupFromSpinner(parentView);
				}
			}
		};
		favouritesAdapter.updateLocation(mapActivity.getMapLocation());
		FavoriteDialogs.showFavoritesDialog(mapActivity, favouritesAdapter, click, dismissListener, dlgHolder, true);
	}

	private boolean isLight() {
		return !nightMode;
	}

	private Drawable getIconOrig(int iconId) {
		IconsCache iconsCache = mapActivity.getMyApplication().getIconsCache();
		return iconsCache.getIcon(iconId, 0, 0f);
	}

	private OnItemClickListener getOnClickListener(final boolean target, final FavouritesAdapter favouritesAdapter,
												   final Dialog[] dlg) {
		return new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				FavouritePoint fp = favouritesAdapter.getItem(position);
				LatLon point = new LatLon(fp.getLatitude(), fp.getLongitude());
				if (target) {
					getTargets().navigateToPoint(point, true, -1, fp.getPointDescription());
				} else {
					getTargets().setStartPoint(point, true, fp.getPointDescription());
				}
				if (dlg != null && dlg.length > 0 && dlg[0] != null) {
					dlg[0].dismiss();
				}
				updateFromIcon();
			}
		};
	}

	public static int getDirectionInfo() {
		return directionInfo;
	}

	public boolean isVisible() {
		return visible;
	}

	public WeakReference<MapRouteInfoMenuFragment> findMenuFragment() {
		Fragment fragment = mapActivity.getSupportFragmentManager().findFragmentByTag(MapRouteInfoMenuFragment.TAG);
		if (fragment != null && !fragment.isDetached()) {
			return new WeakReference<>((MapRouteInfoMenuFragment) fragment);
		} else {
			return null;
		}
	}

	public static boolean isControlVisible() {
		return controlVisible;
	}

	private void updateRouteButtons(final View mainView) {
		mainView.findViewById(R.id.dividerToDropDown).setVisibility(View.VISIBLE);
		mainView.findViewById(R.id.RouteInfoControls).setVisibility(View.VISIBLE);
		final OsmandApplication ctx = mapActivity.getMyApplication();
		ImageView prev = (ImageView) mainView.findViewById(R.id.Prev);
		prev.setImageDrawable(ctx.getIconsCache().getContentIcon(R.drawable.ic_prev, isLight()));
		if (directionInfo >= 0) {
			prev.setVisibility(View.VISIBLE);
			prev.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					if (directionInfo >= 0) {
						directionInfo--;
					}
					if (routingHelper.getRouteDirections() != null && directionInfo >= 0) {
						if (routingHelper.getRouteDirections().size() > directionInfo) {
							RouteDirectionInfo info = routingHelper.getRouteDirections().get(directionInfo);
							net.osmand.Location l = routingHelper.getLocationFromRouteDirection(info);
							contextMenu.showMinimized(new LatLon(l.getLatitude(), l.getLongitude()), null, info);
							mapView.getAnimatedDraggingThread().startMoving(l.getLatitude(), l.getLongitude(),
									mapView.getZoom(), true);
						}
					}
					mapView.refreshMap();
					updateInfo(mainView);
				}

			});
		} else {
			prev.setVisibility(View.GONE);
		}
		ImageView next = (ImageView) mainView.findViewById(R.id.Next);
		next.setVisibility(View.VISIBLE);
		next.setImageDrawable(ctx.getIconsCache().getContentIcon(R.drawable.ic_next, isLight()));
		next.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (routingHelper.getRouteDirections() != null && directionInfo < routingHelper.getRouteDirections().size() - 1) {
					directionInfo++;
					RouteDirectionInfo info = routingHelper.getRouteDirections().get(directionInfo);
					net.osmand.Location l = routingHelper.getLocationFromRouteDirection(info);
					contextMenu.showMinimized(new LatLon(l.getLatitude(), l.getLongitude()), null, info);
					mapView.getAnimatedDraggingThread().startMoving(l.getLatitude(), l.getLongitude(), mapView.getZoom(), true);
				}
				mapView.refreshMap();
				updateInfo(mainView);
			}

		});
		View info = mainView.findViewById(R.id.Info);
		info.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(mapView.getContext(), ShowRouteInfoActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				mapView.getContext().startActivity(intent);
			}
		});

		TextView textView = (TextView) mainView.findViewById(R.id.InfoTextView);
		ImageView infoIcon = (ImageView) mainView.findViewById(R.id.InfoIcon);
		ImageView durationIcon = (ImageView) mainView.findViewById(R.id.DurationIcon);
		View infoDistanceView = mainView.findViewById(R.id.InfoDistance);
		View infoDurationView = mainView.findViewById(R.id.InfoDuration);
		if (directionInfo >= 0) {
			infoIcon.setVisibility(View.GONE);
			durationIcon.setVisibility(View.GONE);
			infoDistanceView.setVisibility(View.GONE);
			infoDurationView.setVisibility(View.GONE);
			textView.setVisibility(View.VISIBLE);
		} else {
			infoIcon.setImageDrawable(ctx.getIconsCache().getContentIcon(R.drawable.ic_action_polygom_dark, isLight()));
			infoIcon.setVisibility(View.VISIBLE);
			durationIcon.setImageDrawable(ctx.getIconsCache().getContentIcon(R.drawable.ic_action_time, isLight()));
			durationIcon.setVisibility(View.VISIBLE);
			infoDistanceView.setVisibility(View.VISIBLE);
			infoDurationView.setVisibility(View.VISIBLE);
			textView.setVisibility(View.GONE);
		}
		if (directionInfo >= 0 && routingHelper.getRouteDirections() != null
				&& directionInfo < routingHelper.getRouteDirections().size()) {
			RouteDirectionInfo ri = routingHelper.getRouteDirections().get(directionInfo);
			textView.setText((directionInfo + 1) + ". " + ri.getDescriptionRoutePart() + " " + OsmAndFormatter.getFormattedDistance(ri.distance, ctx));
		} else {
			TextView distanceText = (TextView) mainView.findViewById(R.id.DistanceText);
			TextView durationText = (TextView) mainView.findViewById(R.id.DurationText);
			distanceText.setText(OsmAndFormatter.getFormattedDistance(ctx.getRoutingHelper().getLeftDistance(), ctx));
			int leftTime = ctx.getRoutingHelper().getLeftTime();
			int hours = leftTime / (60 * 60);
			int minutes = (leftTime / 60) % 60;
			if (hours > 0) {
				durationText.setText(hours + " "
						+ ctx.getString(R.string.osmand_parking_hour)
						+ (minutes > 0 ? " " + minutes + " "
						+ ctx.getString(R.string.osmand_parking_minute) : ""));
			} else {
				durationText.setText(minutes + " "
						+ ctx.getString(R.string.osmand_parking_minute));
			}
		}
	}

	@Override
	public void newRouteIsCalculated(boolean newRoute, ValueHolder<Boolean> showToast) {
		directionInfo = -1;
		updateMenu();
		if (isVisible()) {
			showToast.value = false;
		}
	}

	public String generateViaDescription() {
		TargetPointsHelper targets = getTargets();
		List<TargetPoint> points = targets.getIntermediatePointsNavigation();
		if (points.size() == 0) {
			return "";
		}
		StringBuilder via = new StringBuilder();
		for (int i = 0; i < points.size(); i++) {
			if (i > 0) {
				via.append(" ");
			}
			String description = points.get(i).getOnlyName();
			via.append(getRoutePointDescription(points.get(i).point, description));
			boolean needAddress = new PointDescription(PointDescription.POINT_TYPE_LOCATION, description).isSearchingAddress(mapActivity);
			if (needAddress) {
				AddressLookupRequest lookupRequest = new AddressLookupRequest(points.get(i).point, new GeocodingLookupService.OnAddressLookupResult() {
					@Override
					public void geocodingDone(String address) {
						updateMenu();
					}
				}, null);
				geocodingLookupService.lookupAddress(lookupRequest);
			}
		}
		return via.toString();
	}

	public String getRoutePointDescription(double lat, double lon) {
		return mapActivity.getString(R.string.route_descr_lat_lon, lat, lon);
	}

	public String getRoutePointDescription(LatLon l, String d) {
		if (d != null && d.length() > 0) {
			return d.replace(':', ' ');
		}
		if (l != null) {
			return mapActivity.getString(R.string.route_descr_lat_lon, l.getLatitude(), l.getLongitude());
		}
		return "";
	}

	private Spinner setupFromSpinner(View view) {
		ArrayList<RouteSpinnerRow> fromActions = new ArrayList<>();
		fromActions.add(new RouteSpinnerRow(SPINNER_MY_LOCATION_ID, R.drawable.ic_action_get_my_location,
				mapActivity.getString(R.string.shared_string_my_location)));
		fromActions.add(new RouteSpinnerRow(SPINNER_FAV_ID, R.drawable.ic_action_fav_dark,
				mapActivity.getString(R.string.shared_string_favorite) + mapActivity.getString(R.string.shared_string_ellipsis)));
		fromActions.add(new RouteSpinnerRow(SPINNER_MAP_ID, R.drawable.ic_action_marker_dark,
				mapActivity.getString(R.string.shared_string_select_on_map)));
		fromActions.add(new RouteSpinnerRow(SPINNER_ADDRESS_ID, R.drawable.ic_action_home_dark,
				mapActivity.getString(R.string.shared_string_address) + mapActivity.getString(R.string.shared_string_ellipsis)));

		TargetPoint start = getTargets().getPointToStart();
		if (start != null) {
			String oname = start.getOnlyName().length() > 0 ? start.getOnlyName()
					: (mapActivity.getString(R.string.route_descr_map_location) + " " + getRoutePointDescription(start.getLatitude(), start.getLongitude()));
			fromActions.add(new RouteSpinnerRow(SPINNER_START_ID, R.drawable.ic_action_get_my_location, oname));

			final LatLon latLon = start.point;
			final PointDescription pointDescription = start.getOriginalPointDescription();
			boolean needAddress = pointDescription != null && pointDescription.isSearchingAddress(mapActivity);
			cancelStartPointAddressRequest();
			if (needAddress) {
				startPointRequest = new AddressLookupRequest(latLon, new GeocodingLookupService.OnAddressLookupResult() {
					@Override
					public void geocodingDone(String address) {
						startPointRequest = null;
						updateMenu();
					}
				}, null);
				geocodingLookupService.lookupAddress(startPointRequest);
			}
		}
		final Spinner fromSpinner = ((Spinner) view.findViewById(R.id.FromSpinner));
		RouteSpinnerArrayAdapter fromAdapter = new RouteSpinnerArrayAdapter(view.getContext());
		fromAdapter.addAll(fromActions);
		fromSpinner.setAdapter(fromAdapter);
		if (start != null) {
			fromSpinner.setSelection(fromActions.size() - 1);
		} else {
			if (mapActivity.getMyApplication().getLocationProvider().getLastKnownLocation() == null) {
				fromSpinner.setPromptId(R.string.search_poi_location);
			}
			//fromSpinner.setSelection(0);
		}
		return fromSpinner;
	}

	private Spinner setupToSpinner(View view) {
		final Spinner toSpinner = ((Spinner) view.findViewById(R.id.ToSpinner));
		final TargetPointsHelper targets = getTargets();
		ArrayList<RouteSpinnerRow> toActions = new ArrayList<>();

		TargetPoint finish = getTargets().getPointToNavigate();
		if (finish != null) {
			toActions.add(new RouteSpinnerRow(SPINNER_FINISH_ID, R.drawable.ic_action_get_my_location,
					getRoutePointDescription(targets.getPointToNavigate().point,
							targets.getPointToNavigate().getOnlyName())));

			final LatLon latLon = finish.point;
			final PointDescription pointDescription = finish.getOriginalPointDescription();
			boolean needAddress = pointDescription != null && pointDescription.isSearchingAddress(mapActivity);
			cancelTargetPointAddressRequest();
			if (needAddress) {
				targetPointRequest = new AddressLookupRequest(latLon, new GeocodingLookupService.OnAddressLookupResult() {
					@Override
					public void geocodingDone(String address) {
						targetPointRequest = null;
						updateMenu();
					}
				}, null);
				geocodingLookupService.lookupAddress(targetPointRequest);
			}

		} else {
			toSpinner.setPromptId(R.string.route_descr_select_destination);
			toActions.add(new RouteSpinnerRow(SPINNER_HINT_ID, R.drawable.ic_action_get_my_location,
					mapActivity.getString(R.string.route_descr_select_destination)));
		}
		toActions.add(new RouteSpinnerRow(SPINNER_FAV_ID, R.drawable.ic_action_fav_dark,
				mapActivity.getString(R.string.shared_string_favorite) + mapActivity.getString(R.string.shared_string_ellipsis)));
		toActions.add(new RouteSpinnerRow(SPINNER_MAP_ID, R.drawable.ic_action_marker_dark,
				mapActivity.getString(R.string.shared_string_select_on_map)));
		toActions.add(new RouteSpinnerRow(SPINNER_ADDRESS_ID, R.drawable.ic_action_home_dark,
				mapActivity.getString(R.string.shared_string_address) + mapActivity.getString(R.string.shared_string_ellipsis)));

		RouteSpinnerArrayAdapter toAdapter = new RouteSpinnerArrayAdapter(view.getContext());
		toAdapter.addAll(toActions);
		toSpinner.setAdapter(toAdapter);
		return toSpinner;
	}

	private TargetPointsHelper getTargets() {
		return mapActivity.getMyApplication().getTargetPointsHelper();
	}

	@Override
	public void routeWasCancelled() {
		directionInfo = -1;
		// do not hide fragment (needed for use case entering Planning mode without destination)
	}

	public void onDismiss() {
		visible = false;
		mapActivity.getMapView().setMapPositionX(0);
		mapActivity.getMapView().refreshMap();
		AndroidUiHelper.updateVisibility(mapActivity.findViewById(R.id.map_route_land_left_margin), false);
		AndroidUiHelper.updateVisibility(mapActivity.findViewById(R.id.map_right_widgets_panel), true);
		if (switched) {
			mapControlsLayer.switchToRouteFollowingLayout();
		}
		if (getTargets().getPointToNavigate() == null && !selectFromMapTouch) {
			mapActivity.getMapActions().stopNavigationWithoutConfirm();
		}
	}

	public void show() {
		if (!visible) {
			visible = true;
			switched = mapControlsLayer.switchToRoutePlanningLayout();
			boolean refreshMap = !switched;
			boolean portrait = AndroidUiHelper.isOrientationPortrait(mapActivity);
			if (!portrait) {
				mapActivity.getMapView().setMapPositionX(1);
				refreshMap = true;
			}

			if (refreshMap) {
				mapActivity.refreshMap();
			}

			MapRouteInfoMenuFragment.showInstance(mapActivity);

			if (!AndroidUiHelper.isXLargeDevice(mapActivity)) {
				AndroidUiHelper.updateVisibility(mapActivity.findViewById(R.id.map_right_widgets_panel), false);
			}
			if (!portrait) {
				AndroidUiHelper.updateVisibility(mapActivity.findViewById(R.id.map_route_land_left_margin), true);
			}
		}
	}

	public void hide() {
		WeakReference<MapRouteInfoMenuFragment> fragmentRef = findMenuFragment();
		if (fragmentRef != null) {
			fragmentRef.get().dismiss();
		} else {
			visible = false;
		}
	}

	public void setShowMenu() {
		showMenu = true;
	}

	private class RouteSpinnerRow {
		long id;
		int iconId;
		String text;

		public RouteSpinnerRow(long id, int iconId, String text) {
			this.id = id;
			this.iconId = iconId;
			this.text = text;
		}
	}

	private class RouteSpinnerArrayAdapter extends ArrayAdapter<RouteSpinnerRow> {

		public RouteSpinnerArrayAdapter(Context context) {
			super(context, android.R.layout.simple_spinner_item);
			setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}

		@Override
		public long getItemId(int position) {
			RouteSpinnerRow row = getItem(position);
			return row.id;
		}

		@Override
		public boolean isEnabled(int position) {
			return getItemId(position) != SPINNER_HINT_ID;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			TextView label = (TextView) super.getView(position, convertView, parent);
			RouteSpinnerRow row = getItem(position);
			label.setText(row.text);
			label.setTextColor(!isLight() ?
					ContextCompat.getColorStateList(mapActivity, android.R.color.primary_text_dark) : ContextCompat.getColorStateList(mapActivity, android.R.color.primary_text_light));
			return label;
		}

		@Override
		public View getDropDownView(int position, View convertView, ViewGroup parent) {
			TextView label = (TextView) super.getDropDownView(position, convertView, parent);

			RouteSpinnerRow row = getItem(position);
			long id = getItemId(position);
			label.setText(row.text);
			if (id != SPINNER_HINT_ID) {
				Drawable icon = mapActivity.getMyApplication().getIconsCache().getContentIcon(row.iconId);
				label.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
				label.setCompoundDrawablePadding(AndroidUtils.dpToPx(mapActivity, 16f));
			} else {
				label.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
				label.setCompoundDrawablePadding(0);

			}
			label.setPadding(AndroidUtils.dpToPx(mapActivity, 16f), 0, 0, 0);

			return label;
		}
	}
}
