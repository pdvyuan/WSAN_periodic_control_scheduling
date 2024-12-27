function exetimeCheck
    group = [];
    times = [];
    
    fig = figure;
    set(gca, 'FontSize', 15);
    hold on;
    xlabel('transmissions in a hyper-period');
    ylabel('execution time (ms)');
    
%     t = checkACaseOfExetime(1, 0, fig);
%     times = [times; t];
%     group = [group; repmat('imp/disk', size(t))];
    
    t = checkACaseOfExetime(1, 1, fig);
    times = [times; t];
    group = [group; repmat('implicit  ', size(t))];
    
%     t = checkACaseOfExetime(0, 0, fig);
%     times = [times; t];
%     group = [group; repmat('res/disk', size(t))];
    
    t = checkACaseOfExetime(0, 1, fig);
    times = [times; t];
    group = [group; repmat('restricted', size(t))];
    
    boxplot(times, group);
    fprintf('max time in %d ms\n', max(times));
    ylabel('times');
    figure(fig);
    legend('implicit', 'restricted', 'Location', 'NorthWest');
end

function times = checkACaseOfExetime(implicit, physical, fig)
    % take LLF as example
    algoId = 9;
    if (implicit)
        str = 'implicit deadline';
    else
        str = 'restricted deadline';
    end

    if (physical)
        str = [str ', realistic model'];
    else
        str = [str ', disk model'];
    end

    file = '../log/revision/algorithms/';
    if (implicit)
        file = [file, 'implicit/'];
    else
        file = [file, 'restricted/'];
    end
    if (physical)
        file = [file, 'physical/'];
    else
        file = [file, 'random/'];
    end
    file = [file, 'convert.txt'];

    x = importdata(file);
    data = x.data;
    %find only feasibles ones;
    data = data(data(:, 5*algoId+2)==1, :);
    times = data(:, 5*algoId+3);
    hyperPeriod = data(:, 5*algoId+6);
    fprintf('%s: max=%d, min=%d, mean=%.3f\n', str, max(times), min(times), mean(times));
    flows = data(:, 3);
    utils = data(:, 5);
    channels = data(:, 6);
    
    figure;
    hold on;
    title([str, ' flows vs time']);
    for i=0:4
        ch = 2^i;
        y = [];
        for f = 1:50
            x = times(flows == f & channels == ch);
            if (length(x) ~= 0)
                y = [y, mean(x)];
            else
                y = [y, 0];
            end
        end
        plot(1:50, y);
    end
 
    figure;
    hold on;
    title([str, ' utils vs time']);
    for i=0:4
        ch = 2^i;
        y = [];
        for u=1:16
            x = times(utils > u-1 & utils <= u & channels == ch);
            if (length(x) ~= 0)
                y = [y, mean(x)];
            else
                y = [y, 0];
            end
        end
        plot(0.5:1:16, y);
    end
    
    xs = [];
    y = [];
    ystd = [];
    
    totTx = hyperPeriod .* utils / 10^4;
    maxt = ceil(max(totTx));
    step = 1;
    for t=step:step:maxt
        x = times(totTx > t-step & totTx <= t);
        if (length(x) ~= 0)
            xs = [xs, t-step/2];
            y = [y, mean(x)];
            ystd = [ystd, std(x)];
        end
    end
    
    figure(fig);
    if (implicit)
        if (physical)
            mark = '-^';
        else
            mark = '-o';
        end
    else
        if (physical)
            mark = '-*';
        else
            mark = '-s';
        end
    end
    errorbar(xs*10^4, y, ystd, mark);
    
    y = [];
    for i=0:4
        ch = 2^i;
        x = times(channels == ch);
        if (length(x) ~= 0)
            y = [y, mean(x)];
        else
            y = [y, 0];
        end
    end
    figure;
    plot(2.^[0:4], y);
    title([str, ' channels vs time']);
    
    mat = [];
    for ch=0:4
        arow = [];
        for f = 1:50
            x = times(channels == 2^ch & flows == f);
            if (length(x) ~= 0)
                arow = [arow, max(x)];
            else
                arow = [arow, 0];
            end 
        end
        mat = [mat; arow];
    end
    [X,Y] = meshgrid(0:4, 1:50);
    figure;
    title(str);
    %mesh(X, Y, mat);
    bar3(mat);
end