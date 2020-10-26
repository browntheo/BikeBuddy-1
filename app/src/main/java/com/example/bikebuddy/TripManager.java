package com.example.bikebuddy;

import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import java.util.ArrayList;



public class TripManager {
    private JSONRoutes jsonRoutes;// send requests and show routes on map with this object--PK
    private MapsActivity mapsActivity;
    // Locations for route planning/generating
    private BikeBuddyLocation startingOrigin;
    private BikeBuddyLocation theDestination;

    Boolean startingLocationNeeded = false; // Boolean for telling route initialization the user has no location
    boolean routeStarted = false;//flag determined if a poly line between start and destination markers is drawn or not after map has been cleared

    private Button  removeMarkerButton;
    private Integer clickedMarker;//This value will be the tag value of the clicked marker
    // init data for autocomplete to store
    private LatLng autoCompleteLatLng;
    private GoogleMap mMap;
    private ArrayList<BikeBuddyLocation> locations;



    public TripManager(MapsActivity activity){
        this.mapsActivity = activity;
        locations = new ArrayList<>();
        initMarkerButtons();
    }

    public void initMarkerButtons(){
        removeMarkerButton = (Button) mapsActivity.findViewById(R.id.undoMarkerButton);
        removeMarkerButton.bringToFront();
        removeMarkerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(clickedMarker != null){
                    removeLeg(clickedMarker);
                    showMarkerButtons(false);
                    updateMap();
                }
            }
        });
    }

    public void setUpMapObjects(GoogleMap googleMap){
        this.mMap = googleMap;
        setJSONRoutes(mapsActivity.getResources().getString(R.string.google_maps_key), mMap);
        AsyncGeoCoder.setKey(mapsActivity.getResources().getString(R.string.google_maps_key));
        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            public void onMapLongClick(LatLng latLng) {
                setAutoLatLang(latLng);
                mMap.clear();
                updateMap();
                mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
            }
        });

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                marker.showInfoWindow();
                if(mMap.getCameraPosition().equals(marker.getPosition()))
                    mMap.animateCamera(CameraUpdateFactory.newLatLng(marker.getPosition()));
                updateMarkerTags();
                Integer markerTag = (Integer) marker.getTag();
                if(markerTag != null){
                    setFocusedMarker(markerTag);
                    showMarkerButtons(true);
                    return true;
                }
                return false;
            }
        });

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                showMarkerButtons(false);
            }
        });

        mMap.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {
            @Override
            public void onCameraMove() {
                showMarkerButtons(false);
            }
        });
    }


    public void showMarkerButtons(boolean show){
        if(show)
            removeMarkerButton.setVisibility(View.VISIBLE);
        else
            removeMarkerButton.setVisibility(View.INVISIBLE);
    }

    public void setJSONRoutes(String key, GoogleMap mMap){
        jsonRoutes = new JSONRoutes(key, mMap);
        this.mMap = mMap;
        jsonRoutes.mapsActivity = mapsActivity;
    }

    //LatLng which are generated by long press on the map or from the address entered from the search bar will be input into this function
    //for the input latLang, sets the origin if not already set, if the origin is set,the latLang is used to set the destination
    public void setAutoLatLang(LatLng latLang){
        if(this.locations.size()<6){
        autoCompleteLatLng = latLang;
        if(startingOrigin==null){
            setStartingOrigin(new BikeBuddyLocation(true,latLang, mMap));
            startingOrigin.createMarker();
            startingLocationNeeded = false;
        }else if(theDestination==null){
            setDestination(theDestination = new BikeBuddyLocation(false,latLang, mMap));
            theDestination.setAsDestination();
            theDestination.createMarker();
        }else{//once both origin and destination has been set, all input LatLng will be used to update the destination
            BikeBuddyLocation leg = new BikeBuddyLocation(false, latLang ,mMap);
            if(locations.size() < 3)
                addLeg(leg, 1);
            else
                addLeg(leg, locations.size()-1);
            leg.createMarker();
        }
        if(theDestination != null && mapsActivity.getRouteButton().getVisibility() == View.INVISIBLE)//if the destination has been selected for the first time, then the button will become visible
            mapsActivity.toggleRouteButton();
        updateMarkerTags();
        }else{
            Toast.makeText(mapsActivity,"Limit of 6 Locations per trip",Toast.LENGTH_LONG).show();
        }
    }

    public void showRoute(){
        // locations set, show route
        //if(startingOrigin !=null && theDestination!=null){
        if(locations.size()>1){
            try {
                routeStarted = true; //sets flag so that the polyline for the route will be redrawn if map is cleared
                mMap.clear();
                updateMap();//adds polyline and markers onto map
            }catch (Exception e){
                System.err.println(e);
            }
        }else if(theDestination == null){
            Toast.makeText(mapsActivity, "Please Select Destination", Toast.LENGTH_LONG).show();
        }else{
            Toast.makeText(mapsActivity, "Please Select Origin", Toast.LENGTH_LONG).show();
        }
    }

    public LatLng getAutoCompleteLatLang(){
        return autoCompleteLatLng;
    }
    //redraws all the markers and polyline onto map
    public void updateMap(){
        ArrayList<LatLng> latLngLocations = new ArrayList<>();//list will be used by jsonRoutes to send a request to google directions
        for(BikeBuddyLocation location: locations){//updates/redraws all the location markers on map
            location.update();
            latLngLocations.add(location.getCoordinate());
        }
        if(locations.size()>1 && routeStarted){
            jsonRoutes.setLocations(latLngLocations);
            jsonRoutes.getDirections();
        }
    }
    //sets the starting location to gps location, otherwise sets startingLocationNeeded flag to true
    public void setUpOriginFromLocation(){
        if(mapsActivity.lastKnownLocation==null){
            startingLocationNeeded =true;
        }else{
            LatLng startLatLong = new LatLng(mapsActivity.lastKnownLocation.getLatitude(),mapsActivity.lastKnownLocation.getLongitude());
            startingOrigin = new BikeBuddyLocation(true, startLatLong, mMap);
            startingLocationNeeded = false;
        }
    }
    public BikeBuddyLocation getStartingOrigin() {
        return startingOrigin;
    }

    public BikeBuddyLocation getTheDestination() {
        return theDestination;
    }

    public void setStartingOrigin(BikeBuddyLocation startingOrigin){
        locations.add(0, startingOrigin);
        this.startingOrigin = startingOrigin;
    }

    //last index is replaced by input BikeBikeBuddyLocation, it does NOT add to the list
    public void setDestination(BikeBuddyLocation destination){
        locations.add(destination);
        theDestination = destination;
        theDestination.setAsDestination();
    }

    public void addLeg(BikeBuddyLocation leg, int legNumber){
        locations.add(legNumber, leg);
    }

    public void updateMarkerTags(){
        for(int i=0 ; i<locations.size() ; i++){
            locations.get(i).marker.setTag(i);
        }
    }
    public void removeLeg(int leg){
        boolean removeDestination = false;
        boolean removeOrigin = false;
        if(locations.get(leg).isDestination())//if user wants to remove destination
            removeDestination = true;
        else if(locations.get(leg).isOrigin())
            removeOrigin = true;
        if(leg >= 0 && leg < locations.size()){
            locations.get(leg).setInvisible();//setting the marker invisible before removing from array, else it still appears on map
            locations.remove(leg);
            updateMarkerTags();//tags are updated according to the new order of
        }
        if(removeOrigin || removeDestination){
            resetOriginAndDestination();
        }
        mMap.clear();
        updateMap();
    }

    public void resetOriginAndDestination(){
        if(locations.isEmpty()){//if all markers were removed
            startingOrigin = null; //removing the old reference
            theDestination = null;
            startingLocationNeeded = true;
        }else if(locations.size() == 1){//if only one marker remains
            startingOrigin = locations.get(0);
            startingOrigin.setAsOrigin();
            theDestination = null;
        }else{//if there are atleast 2 markers
            BikeBuddyLocation lastLocation = locations.get(locations.size()-1);
            BikeBuddyLocation firstLocation  = locations.get(0);
            firstLocation.setAsOrigin();
            lastLocation.setAsDestination();
        }
    }

    public void setFocusedMarker(Integer markerTag){
        this.clickedMarker = markerTag;
    }

    public ArrayList<BikeBuddyLocation> getLocations(){
        return locations;
    }

    public int getMarkerIDByLatLong(LatLng latLng){//Marker ID also corrosponds to its Index position in BikeBuddyLocation ArrayList
        updateMarkerTags();
        for(BikeBuddyLocation location: locations){
            if(location.getCoordinate().equals(latLng)){
                return (Integer) location.marker.getTag();
            }
        }
        return 0;
    }

    public Trip getTripDetails(){
        return jsonRoutes.getTrip();
    }

    public JSONRoutes getJsonRoutes(){
        return this.jsonRoutes;
    }

}