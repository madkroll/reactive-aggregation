var stats = {
    type: "GROUP",
name: "Global Information",
path: "",
pathFormatted: "group_missing-name-b06d1",
stats: {
    "name": "Global Information",
    "numberOfRequests": {
        "total": "255930",
        "ok": "255930",
        "ko": "0"
    },
    "minResponseTime": {
        "total": "0",
        "ok": "0",
        "ko": "-"
    },
    "maxResponseTime": {
        "total": "20058",
        "ok": "20058",
        "ko": "-"
    },
    "meanResponseTime": {
        "total": "1431",
        "ok": "1431",
        "ko": "-"
    },
    "standardDeviation": {
        "total": "2387",
        "ok": "2387",
        "ko": "-"
    },
    "percentiles1": {
        "total": "508",
        "ok": "508",
        "ko": "-"
    },
    "percentiles2": {
        "total": "2138",
        "ok": "2138",
        "ko": "-"
    },
    "percentiles3": {
        "total": "10003",
        "ok": "10003",
        "ko": "-"
    },
    "percentiles4": {
        "total": "10036",
        "ok": "10036",
        "ko": "-"
    },
    "group1": {
    "name": "t < 500 ms",
    "count": 122026,
    "percentage": 48
},
    "group2": {
    "name": "500 ms < t < 10100 ms",
    "count": 132351,
    "percentage": 52
},
    "group3": {
    "name": "t > 10100 ms",
    "count": 1553,
    "percentage": 1
},
    "group4": {
    "name": "failed",
    "count": 0,
    "percentage": 0
},
    "meanNumberOfRequestsPerSecond": {
        "total": "284.367",
        "ok": "284.367",
        "ko": "-"
    }
},
contents: {
"req_aggregation-2bb2b": {
        type: "REQUEST",
        name: "aggregation",
path: "aggregation",
pathFormatted: "req_aggregation-2bb2b",
stats: {
    "name": "aggregation",
    "numberOfRequests": {
        "total": "255930",
        "ok": "255930",
        "ko": "0"
    },
    "minResponseTime": {
        "total": "0",
        "ok": "0",
        "ko": "-"
    },
    "maxResponseTime": {
        "total": "20058",
        "ok": "20058",
        "ko": "-"
    },
    "meanResponseTime": {
        "total": "1431",
        "ok": "1431",
        "ko": "-"
    },
    "standardDeviation": {
        "total": "2387",
        "ok": "2387",
        "ko": "-"
    },
    "percentiles1": {
        "total": "508",
        "ok": "508",
        "ko": "-"
    },
    "percentiles2": {
        "total": "2138",
        "ok": "2138",
        "ko": "-"
    },
    "percentiles3": {
        "total": "10003",
        "ok": "10003",
        "ko": "-"
    },
    "percentiles4": {
        "total": "10036",
        "ok": "10036",
        "ko": "-"
    },
    "group1": {
    "name": "t < 500 ms",
    "count": 122026,
    "percentage": 48
},
    "group2": {
    "name": "500 ms < t < 10100 ms",
    "count": 132351,
    "percentage": 52
},
    "group3": {
    "name": "t > 10100 ms",
    "count": 1553,
    "percentage": 1
},
    "group4": {
    "name": "failed",
    "count": 0,
    "percentage": 0
},
    "meanNumberOfRequestsPerSecond": {
        "total": "284.367",
        "ok": "284.367",
        "ko": "-"
    }
}
    }
}

}

function fillStats(stat){
    $("#numberOfRequests").append(stat.numberOfRequests.total);
    $("#numberOfRequestsOK").append(stat.numberOfRequests.ok);
    $("#numberOfRequestsKO").append(stat.numberOfRequests.ko);

    $("#minResponseTime").append(stat.minResponseTime.total);
    $("#minResponseTimeOK").append(stat.minResponseTime.ok);
    $("#minResponseTimeKO").append(stat.minResponseTime.ko);

    $("#maxResponseTime").append(stat.maxResponseTime.total);
    $("#maxResponseTimeOK").append(stat.maxResponseTime.ok);
    $("#maxResponseTimeKO").append(stat.maxResponseTime.ko);

    $("#meanResponseTime").append(stat.meanResponseTime.total);
    $("#meanResponseTimeOK").append(stat.meanResponseTime.ok);
    $("#meanResponseTimeKO").append(stat.meanResponseTime.ko);

    $("#standardDeviation").append(stat.standardDeviation.total);
    $("#standardDeviationOK").append(stat.standardDeviation.ok);
    $("#standardDeviationKO").append(stat.standardDeviation.ko);

    $("#percentiles1").append(stat.percentiles1.total);
    $("#percentiles1OK").append(stat.percentiles1.ok);
    $("#percentiles1KO").append(stat.percentiles1.ko);

    $("#percentiles2").append(stat.percentiles2.total);
    $("#percentiles2OK").append(stat.percentiles2.ok);
    $("#percentiles2KO").append(stat.percentiles2.ko);

    $("#percentiles3").append(stat.percentiles3.total);
    $("#percentiles3OK").append(stat.percentiles3.ok);
    $("#percentiles3KO").append(stat.percentiles3.ko);

    $("#percentiles4").append(stat.percentiles4.total);
    $("#percentiles4OK").append(stat.percentiles4.ok);
    $("#percentiles4KO").append(stat.percentiles4.ko);

    $("#meanNumberOfRequestsPerSecond").append(stat.meanNumberOfRequestsPerSecond.total);
    $("#meanNumberOfRequestsPerSecondOK").append(stat.meanNumberOfRequestsPerSecond.ok);
    $("#meanNumberOfRequestsPerSecondKO").append(stat.meanNumberOfRequestsPerSecond.ko);
}
