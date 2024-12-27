function gw = replaceGateway(sensors, gw)
    global nodes_of_interest;
    nodes_of_interest = sensors;
    options = optimset('GradObj','on', 'Hessian','on', 'Display', 'off');
    gw = fminunc(@sumDist, gw, options);
end