package net.osmand.plus.activities;


import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.osmand.CallbackWithObject;
import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.OsmAndFormatter;
import net.osmand.ResultMatcher;
import net.osmand.access.AccessibleToast;
import net.osmand.data.AmenityType;
import net.osmand.map.ITileSource;
import net.osmand.map.TileSourceManager.TileSourceTemplate;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuAdapter.OnContextMenuClick;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.CommonPreference;
import net.osmand.plus.PoiFilter;
import net.osmand.plus.PoiFiltersHelper;
import net.osmand.plus.R;
import net.osmand.plus.ResourceManager;
import net.osmand.plus.SQLiteTileSource;
import net.osmand.plus.rastermaps.OsmandRasterMapsPlugin;
import net.osmand.plus.render.MapVectorLayer;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.views.ContextMenuLayer;
import net.osmand.plus.views.FavoritesLayer;
import net.osmand.plus.views.GPXLayer;
import net.osmand.plus.views.MapControlsLayer;
import net.osmand.plus.views.MapInfoLayer;
import net.osmand.plus.views.MapTileLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.POIMapLayer;
import net.osmand.plus.views.PointLocationLayer;
import net.osmand.plus.views.PointNavigationLayer;
import net.osmand.plus.views.RouteInfoLayer;
import net.osmand.plus.views.RouteLayer;
import net.osmand.plus.views.TransportInfoLayer;
import net.osmand.plus.views.TransportStopsLayer;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Object is responsible to maintain layers using by map activity 
 */
public class MapActivityLayers {
	
	private final MapActivity activity;
	
	// the order of layer should be preserved ! when you are inserting new layer
	private MapTileLayer mapTileLayer; 
	private MapVectorLayer mapVectorLayer;
	private GPXLayer gpxLayer;
	private RouteLayer routeLayer;
	private POIMapLayer poiMapLayer;
	private FavoritesLayer favoritesLayer;
	private TransportStopsLayer transportStopsLayer;
	private TransportInfoLayer transportInfoLayer;
	private PointLocationLayer locationLayer;
	private PointNavigationLayer navigationLayer;
	private MapInfoLayer mapInfoLayer;
	private ContextMenuLayer contextMenuLayer;
	private RouteInfoLayer routeInfoLayer;
	private MapControlsLayer mapControlsLayer;

	public MapActivityLayers(MapActivity activity) {
		this.activity = activity;
	}

	public OsmandApplication getApplication(){
		return (OsmandApplication) activity.getApplication();
	}
	
	
	public void createLayers(OsmandMapTileView mapView){
		
		RoutingHelper routingHelper = ((OsmandApplication) getApplication()).getRoutingHelper();
		
		// mapView.addLayer(underlayLayer, -0.5f);
		mapTileLayer = new MapTileLayer(true);
		mapView.addLayer(mapTileLayer, 0.0f);
		mapView.setMainLayer(mapTileLayer);
		
		// 0.5 layer
		mapVectorLayer = new MapVectorLayer(mapTileLayer);
		mapView.addLayer(mapVectorLayer, 0.5f);
		
		// mapView.addLayer(overlayLayer, 0.7f);
		
		// 0.9 gpx layer
		gpxLayer = new GPXLayer();
		mapView.addLayer(gpxLayer, 0.9f);
		
		// 1. route layer
		routeLayer = new RouteLayer(routingHelper);
		mapView.addLayer(routeLayer, 1);
		
		// 2. osm bugs layer
		
		// 3. poi layer
		poiMapLayer = new POIMapLayer(activity);
		// 4. favorites layer
		favoritesLayer = new FavoritesLayer();
		// 5. transport layer
		transportStopsLayer = new TransportStopsLayer();
		// 5.5 transport info layer 
		transportInfoLayer = new TransportInfoLayer(TransportRouteHelper.getInstance());
		mapView.addLayer(transportInfoLayer, 5.5f);
		// 6. point location layer 
		locationLayer = new PointLocationLayer();
		mapView.addLayer(locationLayer, 6);
		// 7. point navigation layer
		navigationLayer = new PointNavigationLayer();
		mapView.addLayer(navigationLayer, 7);
		// 8. context menu layer 
		contextMenuLayer = new ContextMenuLayer(activity);
		mapView.addLayer(contextMenuLayer, 8);
		// 9. map info layer
		mapInfoLayer = new MapInfoLayer(activity, routeLayer);
		mapView.addLayer(mapInfoLayer, 9);
		// 10. route info layer
		routeInfoLayer = new RouteInfoLayer(routingHelper, activity, contextMenuLayer);
		mapView.addLayer(routeInfoLayer, 10);
		// 11. route info layer
		mapControlsLayer = new MapControlsLayer(activity);
		mapView.addLayer(mapControlsLayer, 11);
		
		OsmandPlugin.createLayers(mapView, activity);
	}
	
	
	public void updateLayers(OsmandMapTileView mapView){
		OsmandSettings settings = getApplication().getSettings();
		updateMapSource(mapView, settings.MAP_TILE_SOURCES);
		if(mapView.getLayers().contains(transportStopsLayer) != settings.SHOW_TRANSPORT_OVER_MAP.get()){
			if(settings.SHOW_TRANSPORT_OVER_MAP.get()){
				mapView.addLayer(transportStopsLayer, 5);
			} else {
				mapView.removeLayer(transportStopsLayer);
			}
		}
		

		if(mapView.getLayers().contains(poiMapLayer) != settings.SHOW_POI_OVER_MAP.get()){
			if(settings.SHOW_POI_OVER_MAP.get()){
				mapView.addLayer(poiMapLayer, 3);
			} else {
				mapView.removeLayer(poiMapLayer);
			}
		}
		
		if(mapView.getLayers().contains(favoritesLayer) != settings.SHOW_FAVORITES.get()){
			if(settings.SHOW_FAVORITES.get()){
				mapView.addLayer(favoritesLayer, 4);
			} else {
				mapView.removeLayer(favoritesLayer);
			}
		}
		OsmandPlugin.refreshLayers(mapView, activity);
	}
	
	public void updateMapSource(OsmandMapTileView mapView, CommonPreference<String> settingsToWarnAboutMap){
		OsmandSettings settings = getApplication().getSettings();
		
		// update transparency
		int mapTransparency = settings.MAP_UNDERLAY.get() == null ? 255 :  settings.MAP_TRANSPARENCY.get();
		mapTileLayer.setAlpha(mapTransparency);
		mapVectorLayer.setAlpha(mapTransparency);
		
		ITileSource newSource = settings.getMapTileSource(settings.MAP_TILE_SOURCES == settingsToWarnAboutMap);
		ITileSource oldMap = mapTileLayer.getMap();
		if (newSource != oldMap) {
			if (oldMap instanceof SQLiteTileSource) {
				((SQLiteTileSource) oldMap).closeDB();
			}
			mapTileLayer.setMap(newSource);
		}
		
		boolean vectorData = !settings.MAP_ONLINE_DATA.get();
		mapTileLayer.setVisible(!vectorData);
		mapVectorLayer.setVisible(vectorData);
		if(vectorData){
			mapView.setMainLayer(mapVectorLayer);
		} else {
			mapView.setMainLayer(mapTileLayer);
		}
	}

	
	public void openLayerSelectionDialog(final OsmandMapTileView mapView){
		final OsmandSettings settings = getApplication().getSettings();
		final ContextMenuAdapter adapter = new ContextMenuAdapter(activity);
		adapter.registerSelectedItem(R.string.layer_poi, settings.SHOW_POI_OVER_MAP.get() ? 1 : 0, 
				R.drawable.list_activities_poi);
		adapter.registerSelectedItem(R.string.layer_poi_label, settings.SHOW_POI_LABEL.get() ? 1 : 0, 
				0);
		adapter.registerSelectedItem(R.string.layer_favorites, settings.SHOW_FAVORITES.get() ? 1 : 0, 
				R.drawable.list_activities_favorites);
		adapter.registerSelectedItem(R.string.layer_gpx_layer, 
				getApplication().getGpxFileToDisplay() != null ? 1 : 0,  R.drawable.list_activities_gpx_tracks);
		if(routeInfoLayer.couldBeVisible()){
			adapter.registerSelectedItem(R.string.layer_route, 
					routeInfoLayer.isUserDefinedVisible() ? 1 : 0,  0);
		}
		adapter.registerSelectedItem(R.string.layer_transport, settings.SHOW_TRANSPORT_OVER_MAP.get() ? 1 : 0, 
				R.drawable.list_activities_transport_stops);
		if(TransportRouteHelper.getInstance().routeIsCalculated()){
			adapter.registerSelectedItem(R.string.layer_transport_route, 
					routeInfoLayer.isUserDefinedVisible() ? 1 : 0, 0);
		}
		
		
		OsmandPlugin.registerLayerContextMenu(mapView, adapter, activity);
		
		
		final OnMultiChoiceClickListener listener = new DialogInterface.OnMultiChoiceClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int item, boolean isChecked) {
				int itemId = adapter.getItemId(item);
				OnContextMenuClick clck = adapter.getClickAdapter(item);
				if(clck != null) {
					clck.onContextMenuClick(itemId, item, isChecked, dialog);
				} else if(itemId == R.string.layer_poi){
					if(isChecked){
						selectPOIFilterLayer(mapView);
					}
					settings.SHOW_POI_OVER_MAP.set(isChecked);
				} else if(itemId == R.string.layer_poi_label){
					settings.SHOW_POI_LABEL.set(isChecked);
				} else if(itemId == R.string.layer_favorites){
					settings.SHOW_FAVORITES.set(isChecked);
				} else if(itemId == R.string.layer_gpx_layer){
					if(getApplication().getGpxFileToDisplay() != null){
						getApplication().setGpxFileToDisplay(null, false);
					} else {
						dialog.dismiss();
						showGPXFileLayer(mapView);
					}
				} else if(itemId == R.string.layer_route){
					routeInfoLayer.setVisible(isChecked);
				} else if(itemId == R.string.layer_transport_route){
					transportInfoLayer.setVisible(isChecked);
				} else if(itemId == R.string.layer_transport){
					settings.SHOW_TRANSPORT_OVER_MAP.set(isChecked);
				}
				updateLayers(mapView);
				mapView.refreshMap();
			}
		};
		Builder b = new AlertDialog.Builder(activity);
		ListView list = new ListView(activity);
//		list.setBackgroundColor(white);
		list.setCacheColorHint(activity.getResources().getColor(R.color.color_transparent));
		b.setView(list);
		final AlertDialog dlg = b.create();
		final int minWidth = activity.getResources().getDrawable(R.drawable.list_activities_favorites).getMinimumWidth();
		ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(activity, R.layout.layers_list_activity_item, 
				adapter.getItemNames()) {
			@Override
			public View getView(final int position, View convertView, ViewGroup parent) {
				View row = activity.getLayoutInflater().inflate(R.layout.layers_list_activity_item, null);
				((TextView) row.findViewById(R.id.title)).setText(adapter.getItemName(position));
				if(adapter.getImageId(position) != 0) {
					Drawable d = activity.getResources().getDrawable(adapter.getImageId(position));
					((ImageView) row.findViewById(R.id.icon)).setImageDrawable(d);
				} else {
					LinearLayout.LayoutParams layoutParams = (android.widget.LinearLayout.LayoutParams) ((ImageView) row.findViewById(R.id.icon)).getLayoutParams();
					layoutParams.leftMargin = minWidth;
				}
				final CheckBox ch = ((CheckBox) row.findViewById(R.id.check_item));
				if(adapter.getSelection(position) == -1){
					ch.setVisibility(View.INVISIBLE);
				} else {
					ch.setChecked(adapter.getSelection(position) > 0);
					ch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
						@Override
						public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
							listener.onClick(dlg, position, isChecked);
						}
					});
				}
//				row.setOnClickListener(new View.OnClickListener() {
//					@Override
//					public void onClick(View v) {
//						if(selectedList.get(position) >= 0) {
//							ch.setChecked(!ch.isChecked());
//						} else {
//							listener.onClick(dlg, position, selectedList.get(position) > 0);
//						}
//					}
//				});
				return row;
			}
		};
		list.setAdapter(arrayAdapter);
		list.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if(adapter.getSelection(position) >= 0) {
					CheckBox ch = ((CheckBox) view.findViewById(R.id.check_item));
					ch.setChecked(!ch.isChecked());
				} else {
					listener.onClick(dlg, position, adapter.getSelection(position) > 0);
				}
			}
		});
		dlg.setCanceledOnTouchOutside(true);
		dlg.show();
	}
	
	public void showGPXFileLayer(final OsmandMapTileView mapView){
		final OsmandSettings settings = getApplication().getSettings();
		selectGPXFileLayer(new CallbackWithObject<GPXFile>() {
			@Override
			public boolean processResult(GPXFile result) {
				GPXFile toShow = result;
				if (toShow == null) {
					if(!settings.SAVE_TRACK_TO_GPX.get()){
						AccessibleToast.makeText(activity, R.string.gpx_monitoring_disabled_warn, Toast.LENGTH_SHORT).show();
						return true;
					}
					Map<String, GPXFile> data = activity.getSavingTrackHelper().collectRecordedData();
					if(data.isEmpty()){
						toShow = new GPXFile();						
					} else {
						toShow = data.values().iterator().next();
					}
				}
				
				settings.SHOW_FAVORITES.set(true);
				getApplication().setGpxFileToDisplay(toShow, result == null);
				WptPt loc = toShow.findPointToShow();
				if(loc != null){
					mapView.getAnimatedDraggingThread().startMoving(loc.lat, loc.lon, 
							mapView.getZoom(), true);
				}
				mapView.refreshMap();
				return true;
			}
		}, true, true);
	}
	
	public void selectGPXFileLayer(final CallbackWithObject<GPXFile> callbackWithObject, final boolean convertCloudmade,
			final boolean showCurrentGpx) {
		final List<String> list = new ArrayList<String>();
		final OsmandSettings settings = getApplication().getSettings();
		final File dir = settings.extendOsmandPath(ResourceManager.GPX_PATH);
		if (dir != null && dir.canRead()) {
			File[] files = dir.listFiles();
			if (files != null) {
				Arrays.sort(files, new Comparator<File>() {
					@Override
					public int compare(File object1, File object2) {
						if (object1.getName().compareTo(object2.getName()) > 0) {
							return -1;
						} else if (object1.getName().equals(object2.getName())) {
							return 0;
						}
						return 1;
					}

				});

				for (File f : files) {
					if (f.getName().endsWith(".gpx")) { //$NON-NLS-1$
						list.add(f.getName());
					}
				}
			}
		}
		
		if(list.isEmpty()){
			AccessibleToast.makeText(activity, R.string.gpx_files_not_found, Toast.LENGTH_LONG).show();
		}
		if(!list.isEmpty() || showCurrentGpx){
			Builder builder = new AlertDialog.Builder(activity);
			if(showCurrentGpx){
				list.add(0, getString(R.string.show_current_gpx_title));
			}
			builder.setItems(list.toArray(new String[list.size()]), new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
					if(showCurrentGpx && which == 0){
						callbackWithObject.processResult(null);
					} else {
						final ProgressDialog dlg = ProgressDialog.show(activity, getString(R.string.loading),
								getString(R.string.loading_data));
						final File f = new File(dir, list.get(which));
						new Thread(new Runnable() {
							@Override
							public void run() {
								final GPXFile res = GPXUtilities.loadGPXFile(activity, f, convertCloudmade);
								dlg.dismiss();
								activity.runOnUiThread(new Runnable() {
									@Override
									public void run() {
										if (res.warning != null) {
											AccessibleToast.makeText(activity, res.warning, Toast.LENGTH_LONG).show();
										} else {
											callbackWithObject.processResult(res);
										}
									}
								});
							}

						}, "Loading gpx").start(); //$NON-NLS-1$
					}
				}

			});
			builder.show();
		}
	}
	
	private void selectPOIFilterLayer(final OsmandMapTileView mapView){
		final List<PoiFilter> userDefined = new ArrayList<PoiFilter>();
		List<String> list = new ArrayList<String>();
		list.add(getString(R.string.any_poi));
		
		final PoiFiltersHelper poiFilters = ((OsmandApplication)getApplication()).getPoiFilters();
		for (PoiFilter f : poiFilters.getUserDefinedPoiFilters()) {
			if(!f.getFilterId().equals(PoiFilter.BY_NAME_FILTER_ID)){
				userDefined.add(f);
				list.add(f.getName());
			}
		}
		for(AmenityType t : AmenityType.values()){
			list.add(OsmAndFormatter.toPublicString(t, activity));
		}
		Builder builder = new AlertDialog.Builder(activity);
		builder.setItems(list.toArray(new String[list.size()]), new DialogInterface.OnClickListener(){

			@Override
			public void onClick(DialogInterface dialog, int which) {
				String filterId;
				if (which == 0) {
					filterId = PoiFiltersHelper.getOsmDefinedFilterId(null);
				} else if (which <= userDefined.size()) {
					filterId = userDefined.get(which - 1).getFilterId();
				} else {
					filterId = PoiFiltersHelper.getOsmDefinedFilterId(AmenityType.values()[which - userDefined.size() - 1]);
				}
				if(filterId.equals(PoiFilter.CUSTOM_FILTER_ID)){
					Intent newIntent = new Intent(activity, EditPOIFilterActivity.class);
					newIntent.putExtra(EditPOIFilterActivity.AMENITY_FILTER, filterId);
					newIntent.putExtra(EditPOIFilterActivity.SEARCH_LAT, mapView.getLatitude());
					newIntent.putExtra(EditPOIFilterActivity.SEARCH_LON, mapView.getLongitude());
					activity.startActivity(newIntent);
				} else {
					getApplication().getSettings().setPoiFilterForMap(filterId);
					PoiFilter f = poiFilters.getFilterById(filterId);
					if(f != null){
						f.clearNameFilter();
					}
					poiMapLayer.setFilter(f);
					mapView.refreshMap();
				}
			}
			
		});
		builder.show();
	}
	
	public void selectMapLayer(final OsmandMapTileView mapView){
		if(OsmandPlugin.getEnabledPlugin(OsmandRasterMapsPlugin.class) == null) {
			AccessibleToast.makeText(activity, R.string.map_online_plugin_is_not_installed, Toast.LENGTH_LONG).show();
			return;
		}
		final OsmandSettings settings = getApplication().getSettings();
		
		final LinkedHashMap<String, String> entriesMap = new LinkedHashMap<String, String>();
		
		
		final String layerOsmVector = "LAYER_OSM_VECTOR";
		final String layerInstallMore = "LAYER_INSTALL_MORE";
		
		entriesMap.put(layerOsmVector, getString(R.string.vector_data));
		entriesMap.putAll(settings.getTileSourceEntries());
		entriesMap.put(layerInstallMore, getString(R.string.install_more));
		
		final List<Entry<String, String>> entriesMapList = new ArrayList<Entry<String, String>>(entriesMap.entrySet());
		
		Builder builder = new AlertDialog.Builder(activity);
		
		String selectedTileSourceKey = settings.MAP_TILE_SOURCES.get();		

		int selectedItem = -1;
		if (!settings.MAP_ONLINE_DATA.get()) {
			selectedItem = 0;
		} else {
		
			Entry<String, String> selectedEntry = null;
			for (Entry<String, String> entry : entriesMap.entrySet()) {
				if (entry.getKey().equals(selectedTileSourceKey)) {
					selectedEntry = entry;
					break;
				}
			}
			if (selectedEntry != null) {
				selectedItem = 0;
				entriesMapList.remove(selectedEntry);
				entriesMapList.add(0, selectedEntry);
			}
		}
		
		final String[] items = new String[entriesMapList.size()];
		int i = 0;
		for (Entry<String, String> entry : entriesMapList) {
			items[i++] = entry.getValue();
		}
		
		builder.setSingleChoiceItems(items, selectedItem, new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String layerKey = entriesMapList.get(which).getKey();
				if (layerKey.equals(layerOsmVector)) {
					settings.MAP_ONLINE_DATA.set(false);
					updateMapSource(mapView, null);
				} else if (layerKey.equals(layerInstallMore)) {
					SettingsActivity.installMapLayers(activity, new ResultMatcher<TileSourceTemplate>() {
						TileSourceTemplate template = null;
						int count = 0;
						@Override
						public boolean publish(TileSourceTemplate object) {
							if(object == null){
								if(count == 1){
									settings.MAP_TILE_SOURCES.set(template.getName());
									settings.MAP_ONLINE_DATA.set(true);
									updateMapSource(mapView, settings.MAP_TILE_SOURCES);
								} else {
									selectMapLayer(mapView);
								}
							} else {
								count ++;
								template = object;
							}
							return false;
						}
						
						@Override
						public boolean isCancelled() {
							return false;
						}
					});
				} else {
					settings.MAP_TILE_SOURCES.set(layerKey);
					settings.MAP_ONLINE_DATA.set(true);
					updateMapSource(mapView, settings.MAP_TILE_SOURCES);
				}

				dialog.dismiss();
			}
			
		});
		builder.show();
	}

	
	private String getString(int resId) {
		return activity.getString(resId);
	}

	public PointNavigationLayer getNavigationLayer() {
		return navigationLayer;
	}
	
	public GPXLayer getGpxLayer() {
		return gpxLayer;
	}
	
	public ContextMenuLayer getContextMenuLayer() {
		return contextMenuLayer;
	}
	
	public FavoritesLayer getFavoritesLayer() {
		return favoritesLayer;
	}
	public PointLocationLayer getLocationLayer() {
		return locationLayer;
	}
	
	public MapInfoLayer getMapInfoLayer() {
		return mapInfoLayer;
	}
	
	public MapControlsLayer getMapControlsLayer() {
		return mapControlsLayer;
	}
	
	public MapTileLayer getMapTileLayer() {
		return mapTileLayer;
	}
	
	public MapVectorLayer getMapVectorLayer() {
		return mapVectorLayer;
	}
	
	public POIMapLayer getPoiMapLayer() {
		return poiMapLayer;
	}
	
}
