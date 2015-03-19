$(document).ready(function() {
    $("#button").click(function() {
        $.post("/suckdick.api.cgi", null, function(data) {
            $("#button").hide();
            $("#button2").show();
            $("#button2").click(function() {
                alert("Spaghetti international");
            });
        });
    });
});