//var table = document.createElement("TABLE");
var table = document.getElementById("streamtable")

if (typeof (EventSource) !== "undefined") {

                var source = new EventSource("/mongostream",
                        {headers: "Content-Type: text/event-stream"});

                source.onmessage = function (event) {

                    var data = JSON.parse(event.data);

                    var row = table.insertRow(1);

                    var model = row.insertCell(0);
                    var category = row.insertCell(1);
                    var year = row.insertCell(2);
                    var race = row.insertCell(3);
                    var engine = row.insertCell(4);
                    var price = row.insertCell(5);
                    var published = row.insertCell(7);

                    model.innerHTML = data.model
                    category.innerHTML = data.category
                    year.innerHTML = data.year
                    race.innerHTML = Math.floor(data.race_km)
                    engine.innerHTML = Math.floor(data.engine_cubic_cm)
                    price.innerHTML = Math.floor(data.price_usd)
                    published.innerHTML = String(data.published)

                    console.log(data)
                };

            } else {
                document.getElementById("result").innerHTML =
                        "Sorry, your browser does not support server-sent events...";
            }