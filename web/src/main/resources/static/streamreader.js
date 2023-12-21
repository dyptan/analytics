//var table = document.createElement("TABLE");
var table = document.getElementById("streamtable")

if (typeof (EventSource) !== "undefined") {

    var source = new EventSource("/mongostream");

    source.onerror = function (event) {
        console.error("error " + event)
    }
    source.onmessage = function (event) {
        var data = JSON.parse(event.data);
        console.log(data)
        var row = table.insertRow(1);

        var model = row.insertCell(0);
        var vendor = row.insertCell(1);
        var year = row.insertCell(2);
        var race = row.insertCell(3);
        var price = row.insertCell(4);
        // var engine = row.insertCell(4);
        // var published = row.insertCell(7);

        model.innerHTML = data.modelNameEng
        vendor.innerHTML = data.markNameEng
        year.innerHTML = data.autoData.year
        race.innerHTML = Math.floor(data.autoData.raceInt)
        price.innerHTML = Math.floor(data.USD)
        // engine.innerHTML = Math.floor(data.engine_cubic_cm)
        // published.innerHTML = String(data.published)
    };

} else {
    document.getElementById("result").innerHTML =
        "Sorry, your browser does not support server-sent events...";
}