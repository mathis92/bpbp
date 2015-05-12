var map;

function map_initialize() {
    var mapOptions = {
        center: {lat: 48.144773, lng: 17.111540},
        zoom: 13,
        draggableCursor: 'crosshair'
    };
    map = new google.maps.Map(document.getElementById('map-canvas'), mapOptions);
}
google.maps.event.addDomListener(window, 'load', map_initialize);

function fetch_location() {
    $.get("/api/vehicle/39", null, function(data) {
        var coords = data.positionList;
        var myLatlng = new google.maps.LatLng(coords.latitude, coords.longitude);
        var marker = new google.maps.Marker({
            position: myLatlng,
            map: map,
            title: "Buseus 39 locationis ala " + coords.time
        });
        map.setCenter(marker.getPosition());
        /*
         setTimeout(function() {
         fetch_location();
         }, 1500);
         */
    }, "json");
}

var lat;
var lon;
var mapMarkers = [];
var infoWindows = [];
var lastInfowindow = null;

var infoWindowContent = [];
var infowindow = new google.maps.InfoWindow();

var poiArray;
var idToUpdate = "";

$(document).ready(function() {
    $.post("/getPoi", null, function(data) {
        var html = "";
        poiArray = data.poiList;
        for (var i in poiArray) {
            var poi = poiArray[i];            
            html += "<tr>";
            html += "<td>" + (+i + 1) + ".</td>";
            html += "<td><strong>" + poi.title + "</strong></td>";
            html += "<td>" + poi.filePath + "</td>";
            html += "<td>" + poi.lat.toFixed(6) + "</td>";
            html += "<td>" + poi.lon.toFixed(6) + "</td>";
            html += "<td>" + poi.radius + "</td>";
            html += "<td><a href=\"#\" data-poi-goto=\"" + i + "\">Ukázať na mape</a>";
            html += "</tr>";

            var myLatlng = new google.maps.LatLng(poi.lat, poi.lon);
            var marker = new google.maps.Marker({
                position: myLatlng,
                map: map,
                title: poi.title
            });

            mapMarkers[i] = marker;

            infoWindowContent[i] = '<div id="content">' +
                    '<div id="siteNotice">' +
                    '</div>' +
                    '<h4 id="firstHeading" class="firstHeading">Bod ' + poi.title + '</h4>' +
                    '<h5>Priradený k linkám:</h5>' +
                    '<h6>' + poi.routes + '</h6>' +
                    '<h5 style="margin-top: 5px;">Názov video súboru:</h5>' +
                    '<h6>' + poi.filePath + '</h6>' +
                    '</div>' +
                    '<button type="button" onclick="editPoi(' + i + ');" class="btn btn-default" data-dismiss="modal">Upraviť</button>' +
                    '<button style="margin-left: 10px;" type="button" onclick="deletePoi(' + i + ');" class="btn btn-default" data-dismiss="modal">Zmazať</button>';

            google.maps.event.addListener(mapMarkers[i], 'click', function(innerKey) {
                return function() {
                    infowindow.setContent(infoWindowContent[innerKey]);
                    infowindow.open(map, mapMarkers[innerKey]);
                };
            }(i));
        }

        google.maps.event.addListener(map, 'click', function(event) {
            lat = event.latLng.lat();
            lon = event.latLng.lng();
            $('#myModal').modal('show');
            document.getElementById('myModalLabel').innerHTML = '<strong>Vytvoriť bod záujmu pre pozíciu:</strong> ' + event.latLng.lat().toFixed(5) + ', ' + event.latLng.lng().toFixed(5);
            $('#myAlert').hide();
        });

        $("#poiTable tbody").html(html);

        $("#poiTable tbody a[data-poi-goto]").click(function() {
            var num = $(this).attr("data-poi-goto");
            map.setCenter(mapMarkers[num].getPosition());
            infowindow.setContent(infoWindowContent[num]);
            infowindow.open(map, mapMarkers[num]);
        });
    }, "json");
});

function doSubmit() {
    $.ajax({
        url: 'http://bpbp.ctrgn.net/savePoi',
        type: 'POST',
        data: $('#myForm').serialize() + "&lat=" + lat + "&lon=" + lon + "&id=" + idToUpdate,
        success: function(data) {
            alert('successfully submitted');
        }
    });
    $('#myAlert').fadeIn();
    $('#mySubmit').disabled = true;

    setTimeout(function() {
        reloadPage();
    }, 1200);
}

function reloadPage() {    
    location.reload();
    idToUpdate = null;
}

function editPoi(idToEdit) {
    idToUpdate = poiArray[idToEdit].id;
    document.getElementById('poiTitleInput').value = poiArray[idToEdit].title;
    document.getElementById('videoTitleInput').value = poiArray[idToEdit].filePath;
    document.getElementById('routeNumber').value = poiArray[idToEdit].routes;
    lat = poiArray[idToEdit].lat;
    lon = poiArray[idToEdit].lon;
    $('#myModal').modal('show');
    document.getElementById('myModalLabel').innerHTML = '<strong>Upraviť bod záujmu ' + poiArray[idToEdit].title + '</strong>  (pozícia: ' + poiArray[idToEdit].lat.toFixed(5) + ', ' + poiArray[idToEdit].lon.toFixed(5) + ')';
    $('#myAlert').hide();
}

function deletePoi(idToDelete) {
    $.ajax({
        url: 'http://bpbp.ctrgn.net/deletePoi', //this is the submit URL
        type: 'POST', //or POST
        data: "id=" + poiArray[idToDelete].id,
        success: function(data) {
            alert('successfully submitted');
        }
    });
    reloadPage();
}

